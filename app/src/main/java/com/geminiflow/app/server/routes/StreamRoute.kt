package com.geminiflow.app.server.routes

import android.util.Log
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.log.ApiLogRecord
import com.geminiflow.app.domain.model.log.ApiLogRequest
import com.geminiflow.app.domain.model.log.ApiLogResponse
import com.geminiflow.app.domain.model.log.TrafficLog
import com.geminiflow.app.domain.telemetry.classifier.ServerRequestErrorClassifier
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier
import com.geminiflow.app.domain.telemetry.session.RequestTelemetrySession
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.server.dto.ChatRequestDto
import com.geminiflow.app.server.dto.ErrorResponseDto
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.timeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

fun Route.streamRoute(
    streamChatUseCase: StreamChatUseCase,
    trafficLogManager: TrafficLogManager,
    apiLogManager: ApiLogManager,
    serverRequestStatusNotifier: ServerRequestStatusNotifier,
    onRequestStarted: () -> Unit,
    onRequestEnded: () -> Unit
) {
    post("/stream") {
        val t0 = System.currentTimeMillis()
        val clientIp = call.request.local.remoteHost
        val requestId = UUID.randomUUID().toString()
        val session = RequestTelemetrySession(
            requestId = requestId,
            endpoint = "/stream",
            clientIp = clientIp,
            notifier = serverRequestStatusNotifier,
            startTime = t0
        )
        onRequestStarted()

        var promptSummary: String? = null
        try {
            val requestDto = call.receive<ChatRequestDto>()
            promptSummary = requestDto.prompt.take(120)
            if (requestDto.model.isNullOrBlank()) {
                session.markFailed(
                    manualStatusCode = 400,
                    fallbackMessage = "缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型"
                )
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponseDto("缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型")
                )
                RouteUtils.recordErrorTraffic(
                    trafficLogManager,
                    "POST",
                    "/stream",
                    400,
                    t0,
                    clientIp,
                    promptSummary,
                    "缺少模型參數 (model)"
                )
                return@post
            }

            session.markReceived(
                model = requestDto.model,
                promptSnippet = promptSummary
            )
            val domainRequest = RouteUtils.mapToDomainRequest(requestDto)

            val scheme = "http"
            val currentHost = call.request.local.serverHost
            val currentPort = call.request.local.serverPort

            call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header(HttpHeaders.Connection, "keep-alive")

            val fullResponseText = StringBuilder()
            val imagesSaved = mutableListOf<String>()
            var chunkIndex = 0

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                try {
                    @OptIn(FlowPreview::class)
                    streamChatUseCase(domainRequest)
                        .timeout(30.seconds)
                        .collect { chunk ->
                            chunkIndex++
                            if (!chunk.text.isNullOrEmpty()) {
                                fullResponseText.append(chunk.text)
                                val dataJson = buildJsonObject {
                                    put("chunk", chunk.text)
                                }.toString()
                                write("event: text\ndata: $dataJson\n\n")
                                flush()
                            }

                            if (chunk.mediaAsset is MediaAsset.LocalReady) {
                                val filename = (chunk.mediaAsset as MediaAsset.LocalReady).localFile.name
                                val url = "$scheme://$currentHost:$currentPort/images/$filename"
                                imagesSaved.add(url)
                                val dataJson = buildJsonObject {
                                    put("url", url)
                                }.toString()
                                write("event: image\ndata: $dataJson\n\n")
                                flush()
                            }

                            session.notifyStreaming(
                                charCount = fullResponseText.length,
                                chunkIndex = chunkIndex
                            )
                        }

                    write("event: done\ndata: {}\n\n")
                    flush()
                    val streamDuration = System.currentTimeMillis() - t0
                    val fullText = fullResponseText.toString()

                    session.markCompleted(
                        statusCode = 200,
                        totalChars = fullText.length
                    )

                    trafficLogManager.record(
                        TrafficLog(
                            method = "POST",
                            path = "/stream",
                            statusCode = 200,
                            durationMs = streamDuration,
                            clientIp = clientIp,
                            promptSummary = promptSummary,
                            responseSummary = if (fullText.isNotEmpty()) fullText.take(120) else "SSE Stream Complete"
                        )
                    )
                    apiLogManager.logInteraction(
                        ApiLogRecord(
                            timestamp = "",
                            agentName = "AiChat",
                            durationMs = streamDuration,
                            request = ApiLogRequest(
                                model = requestDto.model,
                                prompt = requestDto.prompt,
                                systemPrompt = requestDto.systemPrompt?.takeIf { it.isNotBlank() },
                                sessionId = requestDto.sessionId?.takeIf { it.isNotBlank() },
                                images = requestDto.images.takeIf { it.isNotEmpty() }
                            ),
                            response = ApiLogResponse(
                                text = fullText.takeIf { it.isNotBlank() },
                                images = imagesSaved.takeIf { it.isNotEmpty() }
                            )
                        )
                    )
                } catch (t: Throwable) {
                    val attribution = session.markFailed(t) ?: ServerRequestErrorClassifier.classify(t)
                    // 嘗試向呼叫端發送 SSE 錯誤事件；若連線已中斷（如 Broken pipe / Cancellation），安全忽略次級寫入異常
                    runCatching {
                        val err = buildJsonObject {
                            put("error", attribution.summary)
                            put("status", attribution.statusCode)
                        }.toString()
                        write("event: error\ndata: $err\n\n")
                        flush()
                    }
                    RouteUtils.recordErrorTraffic(
                        trafficLogManager,
                        "POST",
                        "/stream",
                        attribution.statusCode,
                        t0,
                        clientIp,
                        promptSummary,
                        attribution.summary
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("StreamRoute", "Error preparing /stream request: ${e.message}", e)
            val attribution = session.markFailed(e, manualStatusCode = 400)
            val statusCode = attribution?.statusCode ?: 400
            call.respond(HttpStatusCode.fromValue(statusCode), ErrorResponseDto(attribution?.summary ?: (e.message ?: "無效的請求")))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/stream", statusCode, t0, clientIp, promptSummary, e.message)
        } finally {
            session.ensureTerminalState()
            onRequestEnded()
        }
    }
}
