package com.geminiflow.app.data.repository

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.api.GeminiStreamParser
import com.geminiflow.app.data.api.payload.PayloadBuilderFactory
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

class ChatRepositoryImpl(
    private val client: OkHttpClient
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private suspend fun uploadImages(
        images: List<ImagePayload>,
        cookiesHeader: String
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val uploads = mutableListOf<Pair<String, String>>()
        for (img in images) {
            try {
                val startBody = (if (img.filename.isNotEmpty()) "File name: ${img.filename}" else "")
                    .toRequestBody("application/x-www-form-urlencoded;charset=UTF-8".toMediaType())

                val startReqBuilder = Request.Builder()
                    .url(GeminiConfig.UPLOAD_IMAGE_URL)
                    .post(startBody)
                    .header("Cookie", cookiesHeader)

                GeminiConfig.UPLOAD_IMAGE_HEADERS.forEach { (k, v) ->
                    startReqBuilder.header(k, v)
                }
                startReqBuilder.header("size", img.data.size.toString())
                startReqBuilder.header("x-goog-upload-command", "start")

                val startResponse = client.newCall(startReqBuilder.build()).execute()
                val uploadUrl = startResponse.header("X-Goog-Upload-Url")
                startResponse.close()

                if (uploadUrl.isNullOrEmpty()) {
                    throw NetworkException("圖片上傳失敗：缺少 X-Goog-Upload-Url 標頭")
                }

                val uploadBody = img.data.toRequestBody("application/octet-stream".toMediaType())
                val finalizeReqBuilder = Request.Builder()
                    .url(uploadUrl)
                    .post(uploadBody)
                    .header("Cookie", cookiesHeader)

                GeminiConfig.UPLOAD_IMAGE_HEADERS.forEach { (k, v) ->
                    finalizeReqBuilder.header(k, v)
                }
                finalizeReqBuilder.header("size", img.data.size.toString())
                finalizeReqBuilder.header("x-goog-upload-command", "upload, finalize")
                finalizeReqBuilder.header("X-Goog-Upload-Offset", "0")

                val finalizeResponse = client.newCall(finalizeReqBuilder.build()).execute()
                val uploadRef = finalizeResponse.body?.string() ?: ""
                val code = finalizeResponse.code
                finalizeResponse.close()

                if (code >= 400 || uploadRef.isEmpty()) {
                    throw NetworkException("圖片資料上傳失敗：HTTP $code")
                }

                uploads.add(Pair(uploadRef, img.filename))
            } catch (e: Exception) {
                Log.e(TAG, "Image upload error: ${e.message}", e)
                throw if (e is NetworkException) e else NetworkException("上傳圖片失敗: ${e.message}", e)
            }
        }
        uploads
    }

    override suspend fun streamGenerate(
        request: ChatRequest,
        tokens: GeminiTokens,
        cookies: Map<String, String>,
        sessionData: SessionData?
    ): Flow<ChatResponseChunk> = flow {
        val cookiesHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val conversationIds = sessionData?.conversationIds ?: emptyList()

        var uploads = emptyList<Pair<String, String>>()
        if (request.images.isNotEmpty()) {
            uploads = uploadImages(request.images, cookiesHeader)
        }

        val builder = PayloadBuilderFactory.create(request, tokens, uploads, conversationIds)
        val params = builder.buildParams(GeminiConfig.REQUEST_BL_PARAM)
        val payload = builder.buildPayload(GeminiConfig.MODEL_HEADERS)
        val customHeaders = builder.buildHeaders(GeminiConfig.MODEL_HEADERS)

        val urlBuilder = GeminiConfig.GEMINI_REQUEST_URL.toHttpUrlOrNull()?.newBuilder()
            ?: throw NetworkException("無效的 Gemini URL: ${GeminiConfig.GEMINI_REQUEST_URL}")

        params.forEach { (k, v) ->
            urlBuilder.addQueryParameter(k, v)
        }

        val formBuilder = FormBody.Builder()
        payload.forEach { (k, v) ->
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
        customHeaders.forEach { (k, v) ->
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
        var emittedSessionIds = false
        var finalImageCandidate: String? = null
        var fallbackImageCandidate: String? = null

        val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.isBlank()) continue

                val imageCandidates = parser.extractImageCandidates(currentLine)
                for (url in imageCandidates) {
                    when (parser.classifyImageUrl(url)) {
                        "placeholder" -> {
                            if (fallbackImageCandidate == null) fallbackImageCandidate = url
                        }
                        "output" -> {
                            finalImageCandidate = url
                        }
                    }
                }

                val (delta, sessionIds) = parser.extractTextDelta(currentLine)

                if (sessionIds != null && !emittedSessionIds) {
                    emittedSessionIds = true
                    emit(ChatResponseChunk(sessionIds = sessionIds))
                }

                if (!delta.isNullOrEmpty()) {
                    emit(ChatResponseChunk(text = delta))
                }
            }
        } finally {
            reader.close()
            response.close()
        }

        val bestImage = finalImageCandidate ?: fallbackImageCandidate
        if (!bestImage.isNullOrEmpty()) {
            emit(ChatResponseChunk(imageUrl = bestImage))
        }
    }.flowOn(Dispatchers.IO)
}
