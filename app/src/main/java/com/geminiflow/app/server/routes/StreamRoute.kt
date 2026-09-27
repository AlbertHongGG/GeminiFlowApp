package com.geminiflow.app.server.routes

import android.util.Log
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.model.log.TrafficLog
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject

fun Route.streamRoute(
    streamChatUseCase: StreamChatUseCase,
    trafficLogManager: TrafficLogManager,
    apiLogManager: ApiLogManager,
    onRequestStarted: () -> Unit,
    onRequestEnded: () -> Unit
) {
    post("/stream") {
        val t0 = System.currentTimeMillis()
        val clientIp = call.request.local.remoteHost
        onRequestStarted()

        var promptSummary: String? = null
        try {
            val requestDto = call.receive<ChatRequestDto>()
            promptSummary = requestDto.prompt.take(120)
            if (requestDto.model.isNullOrBlank()) {
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
            val domainRequest = RouteUtils.mapToDomainRequest(requestDto)

            val scheme = "http"
            val currentHost = call.request.local.serverHost
            val currentPort = call.request.local.serverPort

            call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header(HttpHeaders.Connection, "keep-alive")

            val fullResponseText = StringBuilder()
            val imagesSaved = mutableListOf<String>()

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                try {
                    streamChatUseCase(domainRequest).collect { chunk ->
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
                    }

                    write("event: done\ndata: {}\n\n")
                    flush()
                    val streamDuration = System.currentTimeMillis() - t0
                    val fullText = fullResponseText.toString()

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
                    val reqJson = JSONObject().apply {
                        put("model", requestDto.model)
                        put("prompt", requestDto.prompt)
                        if (!requestDto.systemPrompt.isNullOrEmpty()) put("system_prompt", requestDto.systemPrompt)
                        if (requestDto.sessionId != null) put("session_id", requestDto.sessionId)
                        if (requestDto.images.isNotEmpty()) put("images_count", requestDto.images.size)
                    }
                    val respJson = JSONObject().apply {
                        put("text", fullText)
                        if (imagesSaved.isNotEmpty()) {
                            put("images", JSONArray(imagesSaved))
                        }
                        put("status", "SSE Stream Complete")
                    }
                    apiLogManager.logInteraction(
                        agentName = "AiChat",
                        durationMs = streamDuration,
                        request = reqJson,
                        response = respJson
                    )
                } catch (e: AuthenticationRequiredException) {
                    val err = buildJsonObject {
                        put("error", e.message ?: "未授權")
                        put("status", 401)
                    }.toString()
                    write("event: error\ndata: $err\n\n")
                    flush()
                    RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/stream", 401, t0, clientIp, promptSummary, e.message)
                } catch (e: Exception) {
                    val err = buildJsonObject {
                        put("error", e.message ?: "串流處理錯誤")
                        put("status", 500)
                    }.toString()
                    write("event: error\ndata: $err\n\n")
                    flush()
                    RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/stream", 500, t0, clientIp, promptSummary, e.message)
                }
            }
        } catch (e: Exception) {
            Log.e("StreamRoute", "Error preparing /stream request: ${e.message}", e)
            call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("無效的請求格式: ${e.message}"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/stream", 400, t0, clientIp, promptSummary, e.message)
        } finally {
            onRequestEnded()
        }
    }
}
