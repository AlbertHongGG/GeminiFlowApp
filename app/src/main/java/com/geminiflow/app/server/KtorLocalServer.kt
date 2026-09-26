package com.geminiflow.app.server

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.ImageStorageManager
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.AuthenticationRequiredException
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.ImagePayload
import com.geminiflow.app.domain.model.MediaAsset
import com.geminiflow.app.domain.model.NetworkException
import com.geminiflow.app.domain.model.PayloadException
import com.geminiflow.app.domain.model.ServerStatus
import com.geminiflow.app.domain.model.TokenExpiredException
import com.geminiflow.app.domain.model.TrafficLog
import com.geminiflow.app.domain.usecase.StreamChatUseCase
import com.geminiflow.app.server.dto.ChatRequestDto
import com.geminiflow.app.server.dto.ChatResponseDto
import com.geminiflow.app.server.dto.ErrorResponseDto
import com.geminiflow.app.server.dto.HealthResponseDto
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class KtorLocalServer(
    private val streamChatUseCase: StreamChatUseCase,
    private val imageStorageManager: ImageStorageManager,
    private val trafficLogManager: TrafficLogManager,
    private val apiLogManager: ApiLogManager
) {
    companion object {
        private const val TAG = "KtorLocalServer"
    }

    private var serverEngine: CIOApplicationEngine? = null

    private val _status = MutableStateFlow(ServerStatus())
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val totalRequests = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)

    val isRunning: Boolean
        get() = serverEngine != null && _status.value.isRunning

    @Synchronized
    fun start(host: String = "127.0.0.1", port: Int = 5000) {
        if (serverEngine != null) {
            Log.w(TAG, "Server is already running.")
            return
        }

        try {
            _status.value = _status.value.copy(
                host = host,
                port = port,
                errorMessage = null
            )

            val engine = embeddedServer(CIO, host = host, port = port) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }

                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                }

                routing {
                    get("/health") {
                        val t0 = System.currentTimeMillis()
                        val clientIp = call.request.local.remoteHost
                        call.respond(HttpStatusCode.OK, HealthResponseDto(ok = true))
                        trafficLogManager.record(
                            TrafficLog(
                                method = "GET",
                                path = "/health",
                                statusCode = 200,
                                durationMs = System.currentTimeMillis() - t0,
                                clientIp = clientIp,
                                responseSummary = "ok: true"
                            )
                        )
                    }

                    get("/images/{filename}") {
                        val t0 = System.currentTimeMillis()
                        val clientIp = call.request.local.remoteHost
                        val filename = call.parameters["filename"]
                        if (filename.isNullOrBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("缺少圖片名稱"))
                            trafficLogManager.record(
                                TrafficLog(
                                    method = "GET",
                                    path = "/images/empty",
                                    statusCode = 400,
                                    durationMs = System.currentTimeMillis() - t0,
                                    clientIp = clientIp,
                                    responseSummary = "缺少圖片名稱"
                                )
                            )
                            return@get
                        }
                        val imageFile = imageStorageManager.getImageFile(filename)
                        if (imageFile != null && imageFile.exists()) {
                            call.respondFile(imageFile)
                            trafficLogManager.record(
                                TrafficLog(
                                    method = "GET",
                                    path = "/images/$filename",
                                    statusCode = 200,
                                    durationMs = System.currentTimeMillis() - t0,
                                    clientIp = clientIp,
                                    responseSummary = "Image sent (${imageFile.length()} bytes)"
                                )
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, ErrorResponseDto("找不到該圖片"))
                            trafficLogManager.record(
                                TrafficLog(
                                    method = "GET",
                                    path = "/images/$filename",
                                    statusCode = 404,
                                    durationMs = System.currentTimeMillis() - t0,
                                    clientIp = clientIp,
                                    responseSummary = "找不到該圖片"
                                )
                            )
                        }
                    }

                    post("/chat") {
                        val t0 = System.currentTimeMillis()
                        val clientIp = call.request.local.remoteHost
                        totalRequests.incrementAndGet()
                        activeConnections.incrementAndGet()
                        updateStatusCounts()

                        var promptSummary: String? = null
                        try {
                            val requestDto = call.receive<ChatRequestDto>()
                            promptSummary = requestDto.prompt.take(120)
                            if (requestDto.model.isNullOrBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponseDto("缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型")
                                )
                                recordErrorTraffic(
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
                            val domainRequest = mapToDomainRequest(requestDto)

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
                            recordErrorTraffic("POST", "/chat", 401, t0, clientIp, promptSummary, e.message)
                        } catch (e: TokenExpiredException) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponseDto(e.message ?: "憑證過期"))
                            recordErrorTraffic("POST", "/chat", 401, t0, clientIp, promptSummary, e.message)
                        } catch (e: NetworkException) {
                            call.respond(HttpStatusCode.BadGateway, ErrorResponseDto(e.message ?: "網路連線異常"))
                            recordErrorTraffic("POST", "/chat", 502, t0, clientIp, promptSummary, e.message)
                        } catch (e: PayloadException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponseDto(e.message ?: "請求內容錯誤"))
                            recordErrorTraffic("POST", "/chat", 422, t0, clientIp, promptSummary, e.message)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling /chat request: ${e.message}", e)
                            call.respond(HttpStatusCode.InternalServerError, ErrorResponseDto(e.message ?: "伺服器內部錯誤"))
                            recordErrorTraffic("POST", "/chat", 500, t0, clientIp, promptSummary, e.message)
                        } finally {
                            activeConnections.decrementAndGet()
                            updateStatusCounts()
                        }
                    }

                    post("/stream") {
                        val t0 = System.currentTimeMillis()
                        val clientIp = call.request.local.remoteHost
                        totalRequests.incrementAndGet()
                        activeConnections.incrementAndGet()
                        updateStatusCounts()

                        var promptSummary: String? = null
                        try {
                            val requestDto = call.receive<ChatRequestDto>()
                            promptSummary = requestDto.prompt.take(120)
                            if (requestDto.model.isNullOrBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponseDto("缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型")
                                )
                                recordErrorTraffic(
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
                            val domainRequest = mapToDomainRequest(requestDto)

                            val scheme = "http"
                            val currentHost = call.request.local.serverHost
                            val currentPort = call.request.local.serverPort

                            call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
                            call.response.header(HttpHeaders.CacheControl, "no-cache")
                            call.response.header(HttpHeaders.Connection, "keep-alive")

                            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                                try {
                                    streamChatUseCase(domainRequest).collect { chunk ->
                                        if (!chunk.text.isNullOrEmpty()) {
                                            val dataJson = buildJsonObject {
                                                put("chunk", chunk.text)
                                            }.toString()
                                            write("event: text\ndata: $dataJson\n\n")
                                            flush()
                                        }

                                        if (chunk.mediaAsset is MediaAsset.LocalReady) {
                                            val filename = (chunk.mediaAsset as MediaAsset.LocalReady).localFile.name
                                            val url = "$scheme://$currentHost:$currentPort/images/$filename"
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
                                    trafficLogManager.record(
                                        TrafficLog(
                                            method = "POST",
                                            path = "/stream",
                                            statusCode = 200,
                                            durationMs = streamDuration,
                                            clientIp = clientIp,
                                            promptSummary = promptSummary,
                                            responseSummary = "SSE Stream Complete"
                                        )
                                    )
                                    val reqJson = JSONObject().apply {
                                        put("model", requestDto.model)
                                        put("prompt", requestDto.prompt)
                                        if (!requestDto.systemPrompt.isNullOrEmpty()) put("system_prompt", requestDto.systemPrompt)
                                        if (requestDto.sessionId != null) put("session_id", requestDto.sessionId)
                                    }
                                    val respJson = JSONObject().apply {
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
                                    recordErrorTraffic("POST", "/stream", 401, t0, clientIp, promptSummary, e.message)
                                } catch (e: Exception) {
                                    val err = buildJsonObject {
                                        put("error", e.message ?: "串流處理錯誤")
                                        put("status", 500)
                                    }.toString()
                                    write("event: error\ndata: $err\n\n")
                                    flush()
                                    recordErrorTraffic("POST", "/stream", 500, t0, clientIp, promptSummary, e.message)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error preparing /stream request: ${e.message}", e)
                            call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("無效的請求格式: ${e.message}"))
                            recordErrorTraffic("POST", "/stream", 400, t0, clientIp, promptSummary, e.message)
                        } finally {
                            activeConnections.decrementAndGet()
                            updateStatusCounts()
                        }
                    }
                }
            }

            engine.start(wait = false)
            serverEngine = engine
            _status.value = _status.value.copy(
                isRunning = true,
                host = host,
                port = port,
                errorMessage = null,
                startTime = System.currentTimeMillis()
            )
            Log.i(TAG, "Ktor Embedded Server started on http://$host:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Ktor server: ${e.message}", e)
            _status.value = _status.value.copy(
                isRunning = false,
                errorMessage = e.message,
                startTime = null
            )
            throw e
        }
    }

    @Synchronized
    fun stop() {
        serverEngine?.let {
            it.stop(1000, 2000)
            serverEngine = null
            _status.value = _status.value.copy(
                isRunning = false,
                activeConnections = 0,
                startTime = null
            )
            Log.i(TAG, "Ktor Embedded Server stopped.")
        }
    }

    private fun recordErrorTraffic(
        method: String,
        path: String,
        statusCode: Int,
        startTime: Long,
        clientIp: String,
        prompt: String?,
        error: String?
    ) {
        trafficLogManager.record(
            TrafficLog(
                method = method,
                path = path,
                statusCode = statusCode,
                durationMs = System.currentTimeMillis() - startTime,
                clientIp = clientIp,
                promptSummary = prompt,
                responseSummary = error ?: "HTTP $statusCode"
            )
        )
    }

    private fun updateStatusCounts() {
        _status.value = _status.value.copy(
            totalRequests = totalRequests.get(),
            activeConnections = activeConnections.get()
        )
    }

    private fun mapToDomainRequest(dto: ChatRequestDto): ChatRequest {
        val model = dto.model?.takeIf { it.isNotBlank() }
            ?: throw PayloadException("缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型")

        val imagePayloads = dto.images.mapIndexed { index, rawStr ->
            decodeBase64Image(rawStr, index)
        }

        return ChatRequest(
            prompt = dto.prompt,
            systemPrompt = dto.systemPrompt,
            model = model,
            language = dto.language,
            images = imagePayloads,
            sessionId = dto.sessionId,
            autoRefreshCookies = dto.autoRefreshCookies
        )
    }

    private fun decodeBase64Image(value: String, index: Int): ImagePayload {
        var base64Part = value
        var ext = "png"

        if (value.startsWith("data:image/")) {
            val commaIndex = value.indexOf(",")
            if (commaIndex != -1) {
                val header = value.substring(0, commaIndex)
                base64Part = value.substring(commaIndex + 1)
                val mime = header.substringAfter("data:").substringBefore(";")
                ext = when (mime.lowercase()) {
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/webp" -> "webp"
                    else -> "png"
                }
            }
        }

        val cleaned = base64Part.replace("\\s+".toRegex(), "")
        val bytes = Base64.decode(cleaned, Base64.DEFAULT)
        return ImagePayload(data = bytes, filename = "upload_${index + 1}.$ext")
    }
}
