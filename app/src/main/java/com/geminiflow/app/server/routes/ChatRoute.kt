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
import com.geminiflow.app.server.dto.ChatResponseDto
import com.geminiflow.app.server.dto.ErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.timeout
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

fun Route.chatRoute(
    streamChatUseCase: StreamChatUseCase,
    trafficLogManager: TrafficLogManager,
    apiLogManager: ApiLogManager,
    serverRequestStatusNotifier: ServerRequestStatusNotifier,
    onRequestStarted: () -> Unit,
    onRequestEnded: () -> Unit
) {
    post("/chat") {
        val t0 = System.currentTimeMillis()
        val clientIp = call.request.local.remoteHost
        val requestId = UUID.randomUUID().toString()
        val session = RequestTelemetrySession(
            requestId = requestId,
            endpoint = "/chat",
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
                    "/chat",
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

            val textParts = StringBuilder()
            val imagesSaved = mutableListOf<String>()

            val scheme = "http"
            val currentHost = call.request.local.serverHost
            val currentPort = call.request.local.serverPort

            @OptIn(FlowPreview::class)
            streamChatUseCase(domainRequest)
                .timeout(120.seconds)
                .collect { chunk ->
                    if (!chunk.text.isNullOrEmpty()) {
                        textParts.append(chunk.text)
                    }
                    if (chunk.mediaAsset is MediaAsset.LocalReady) {
                        val filename = (chunk.mediaAsset as MediaAsset.LocalReady).localFile.name
                        imagesSaved.add("$scheme://$currentHost:$currentPort/images/$filename")
                    }
                }

            val fullText = textParts.toString()
            val duration = System.currentTimeMillis() - t0

            session.markCompleted(
                statusCode = 200,
                totalChars = fullText.length
            )

            call.respond(
                HttpStatusCode.OK,
                ChatResponseDto(text = fullText, images = imagesSaved)
            )
            trafficLogManager.record(
                TrafficLog(
                    method = "POST",
                    path = "/chat",
                    statusCode = 200,
                    durationMs = duration,
                    clientIp = clientIp,
                    promptSummary = promptSummary,
                    responseSummary = fullText.take(120)
                )
            )
            apiLogManager.logInteraction(
                ApiLogRecord(
                    timestamp = "",
                    agentName = "AiChat",
                    durationMs = duration,
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
            Log.e("ChatRoute", "Error handling /chat request: ${t.message}", t)
            val attribution = session.markFailed(t) ?: ServerRequestErrorClassifier.classify(t)
            val statusCode = HttpStatusCode.fromValue(attribution.statusCode)
            call.respond(statusCode, ErrorResponseDto(attribution.summary))
            RouteUtils.recordErrorTraffic(
                trafficLogManager,
                "POST",
                "/chat",
                attribution.statusCode,
                t0,
                clientIp,
                promptSummary,
                attribution.summary
            )
        } finally {
            session.ensureTerminalState()
            onRequestEnded()
        }
    }
}
