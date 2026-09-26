package com.geminiflow.app.server.routes

import android.util.Log
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.model.common.NetworkException
import com.geminiflow.app.domain.model.common.PayloadException
import com.geminiflow.app.domain.model.common.TokenExpiredException
import com.geminiflow.app.domain.model.log.TrafficLog
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
import org.json.JSONArray
import org.json.JSONObject

fun Route.chatRoute(
    streamChatUseCase: StreamChatUseCase,
    trafficLogManager: TrafficLogManager,
    apiLogManager: ApiLogManager,
    onRequestStarted: () -> Unit,
    onRequestEnded: () -> Unit
) {
    post("/chat") {
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
                    "/chat",
                    400,
                    t0,
                    clientIp,
                    promptSummary,
                    "缺少模型參數 (model)"
                )
                return@post
            }
            val domainRequest = RouteUtils.mapToDomainRequest(requestDto)

            val textParts = StringBuilder()
            val imagesSaved = mutableListOf<String>()

            val scheme = "http"
            val currentHost = call.request.local.serverHost
            val currentPort = call.request.local.serverPort

            streamChatUseCase(domainRequest).collect { chunk ->
                if (!chunk.text.isNullOrEmpty()) {
                    textParts.append(chunk.text)
                }
                if (chunk.mediaAsset is MediaAsset.LocalReady) {
                    val filename = (chunk.mediaAsset as MediaAsset.LocalReady).localFile.name
                    imagesSaved.add("$scheme://$currentHost:$currentPort/images/$filename")
                }
            }

            val fullText = textParts.toString()
            call.respond(
                HttpStatusCode.OK,
                ChatResponseDto(text = fullText, images = imagesSaved)
            )
            val duration = System.currentTimeMillis() - t0
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
            val reqJson = JSONObject().apply {
                put("model", requestDto.model)
                put("prompt", requestDto.prompt)
                if (!requestDto.systemPrompt.isNullOrEmpty()) put("system_prompt", requestDto.systemPrompt)
                if (requestDto.sessionId != null) put("session_id", requestDto.sessionId)
                if (requestDto.images.isNotEmpty()) put("images_count", requestDto.images.size)
            }
            val respJson = JSONObject().apply {
                put("text", fullText)
                put("images", JSONArray(imagesSaved))
            }
            apiLogManager.logInteraction(
                agentName = "AiChat",
                durationMs = duration,
                request = reqJson,
                response = respJson
            )
        } catch (e: AuthenticationRequiredException) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponseDto(e.message ?: "未授權"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/chat", 401, t0, clientIp, promptSummary, e.message)
        } catch (e: TokenExpiredException) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponseDto(e.message ?: "憑證過期"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/chat", 401, t0, clientIp, promptSummary, e.message)
        } catch (e: NetworkException) {
            call.respond(HttpStatusCode.BadGateway, ErrorResponseDto(e.message ?: "網路連線異常"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/chat", 502, t0, clientIp, promptSummary, e.message)
        } catch (e: PayloadException) {
            call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponseDto(e.message ?: "請求內容錯誤"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/chat", 422, t0, clientIp, promptSummary, e.message)
        } catch (e: Exception) {
            Log.e("ChatRoute", "Error handling /chat request: ${e.message}", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponseDto(e.message ?: "伺服器內部錯誤"))
            RouteUtils.recordErrorTraffic(trafficLogManager, "POST", "/chat", 500, t0, clientIp, promptSummary, e.message)
        } finally {
            onRequestEnded()
        }
    }
}
