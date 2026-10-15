package com.geminiflow.app.data.repository

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.api.GeminiStreamParser
import com.geminiflow.app.data.api.protocol.GeminiProtocolEngine
import com.geminiflow.app.data.network.RequestTelemetryTag
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionData
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ChatResponseChunk
import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.domain.model.common.NetworkException
import com.geminiflow.app.domain.model.common.TokenExpiredException
import com.geminiflow.app.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

import com.geminiflow.app.data.network.upload.GeminiImageUploader
import com.geminiflow.app.domain.attestation.TokenSigner
import com.geminiflow.app.domain.model.common.RateLimitException
import com.geminiflow.app.domain.repository.ImageUploader

class ChatRepositoryImpl(
    private val client: OkHttpClient,
    private val protocolEngine: GeminiProtocolEngine = GeminiProtocolEngine(GeminiConfig.REQUEST_BL_PARAM),
    private val tokenSigner: TokenSigner? = null,
    private val imageUploader: ImageUploader = GeminiImageUploader(client)
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    override suspend fun streamGenerate(
        request: ChatRequest,
        tokens: GeminiTokens,
        cookies: Map<String, String>,
        sessionData: SessionData?
    ): Flow<ChatResponseChunk> = flow {
        val cookiesHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val uploads = if (request.images.isNotEmpty()) {
            imageUploader.uploadImages(request.images, cookies)
        } else {
            emptyList()
        }

        // 2. Cryptographic Botguard Attestation Signing (Content-Bound)
        var botguardToken: String? = null
        var botguardSalt: String? = null
        if (tokenSigner != null) {
            try {
                val bgToken = tokenSigner.sign(request.effectivePrompt, sessionData?.conversationId)
                if (bgToken != null) {
                    botguardToken = bgToken.token
                    botguardSalt = bgToken.salt
                }
            } catch (e: Exception) {
                Log.w(TAG, "Token signer execution failed, proceeding without Botguard token: ${e.message}")
            }
        }

        val protoReq = protocolEngine.buildRequest(
            request = request,
            tokens = tokens,
            sessionData = sessionData,
            uploads = uploads,
            botguardToken = botguardToken,
            botguardSalt = botguardSalt
        )

        val urlBuilder = GeminiConfig.GEMINI_REQUEST_URL.toHttpUrlOrNull()?.newBuilder()
            ?: throw NetworkException("無效的 Gemini URL: ${GeminiConfig.GEMINI_REQUEST_URL}")

        protoReq.params.forEach { (k, v) ->
            urlBuilder.addQueryParameter(k, v)
        }

        val formBuilder = FormBody.Builder()
        protoReq.postData.forEach { (k, v) ->
            formBuilder.add(k, v)
        }
        val formBody = formBuilder.build()

        val reqBuilder = Request.Builder()
            .url(urlBuilder.build())
            .post(formBody)
            .header("Cookie", cookiesHeader)

        GeminiConfig.DEFAULT_HTTP_HEADERS.forEach { (k, v) ->
            reqBuilder.header(k, v)
        }
        protoReq.headers.forEach { (k, v) ->
            reqBuilder.header(k, v)
        }

        val promptSummary = request.prompt.lines().firstOrNull()?.trim()?.take(50) ?: "Gemini Chat"
        reqBuilder.tag(
            RequestTelemetryTag::class.java,
            RequestTelemetryTag(
                promptSummary = promptSummary,
                source = request.model
            )
        )

        val response = try {
            client.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            throw NetworkException("連線至 Gemini 伺服器失敗: ${e.message}", e)
        }

        if (response.code == 429) {
            val bodyPreview = response.body?.string()?.take(150)
            response.close()
            throw RateLimitException("Google 邊緣網關觸發頻率限制 (HTTP 429): $bodyPreview")
        }

        if (response.code in listOf(401, 403)) {
            val bodyPreview = response.body?.string()?.take(150)
            response.close()
            throw TokenExpiredException("Google 授權憑證失效 (HTTP ${response.code}): $bodyPreview")
        }

        if (response.code >= 400) {
            val bodyPreview = response.body?.string()?.take(300)
            response.close()
            throw NetworkException("Gemini API 回傳錯誤 HTTP ${response.code}: $bodyPreview")
        }

        val responseBody = response.body
            ?: throw NetworkException("Gemini API 回應內容為空")

        val parser = GeminiStreamParser()
        var emittedSession = false
        var latestConversationId: String? = sessionData?.conversationId
        var latestContinuationToken: String? = sessionData?.continuationToken
        var lastEmittedContToken: String? = null
        var latestExportMetadata: com.geminiflow.app.domain.model.export.ImageExportMetadata? = null

        val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.isBlank()) continue

                val exportMeta = parser.extractImageExportMetadata(currentLine)
                if (exportMeta != null) {
                    latestExportMetadata = exportMeta
                }

                val (delta, convId, contToken) = parser.extractTextDelta(currentLine)

                if (convId != null) {
                    latestConversationId = convId
                }
                if (contToken != null) {
                    latestContinuationToken = contToken
                }

                if (latestConversationId != null && !emittedSession) {
                    emittedSession = true
                    emit(
                        ChatResponseChunk(
                            conversationId = latestConversationId,
                            continuationToken = latestContinuationToken
                        )
                    )
                    lastEmittedContToken = latestContinuationToken
                }

                if (!delta.isNullOrEmpty()) {
                    emit(ChatResponseChunk(text = delta))
                }

                // 核心防延遲機制：一旦命中結束訊框，立刻主動中斷讀取迴圈！
                if (parser.isEndOfStream(currentLine)) {
                    break
                }
            }
        } finally {
            try {
                reader.close()
                response.close()
            } catch (_: Exception) {
            }
        }

        // 若續接 Token 晚到，發出最後一次 Session 補發 Chunk
        if (latestConversationId != null && latestContinuationToken != null && latestContinuationToken != lastEmittedContToken) {
            emit(
                ChatResponseChunk(
                    conversationId = latestConversationId,
                    continuationToken = latestContinuationToken
                )
            )
        } else if (!emittedSession && (latestConversationId != null || latestContinuationToken != null)) {
            emit(
                ChatResponseChunk(
                    conversationId = latestConversationId,
                    continuationToken = latestContinuationToken
                )
            )
        }

        if (latestExportMetadata != null) {
            emit(
                ChatResponseChunk(
                    imageUrl = latestExportMetadata.previewUrl,
                    conversationId = latestConversationId,
                    continuationToken = latestContinuationToken,
                    exportMetadata = latestExportMetadata
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
