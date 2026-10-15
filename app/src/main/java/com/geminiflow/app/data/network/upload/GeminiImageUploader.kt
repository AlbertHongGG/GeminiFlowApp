package com.geminiflow.app.data.network.upload

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.domain.model.common.NetworkException
import com.geminiflow.app.domain.repository.ImageUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Google Gemini 官方多模態 Scotty Resumable Upload 協定實作。
 * 遵循兩階段上傳機制：
 *   1. 會話初始化 (Start Session)：宣告檔案大小並取得專屬動態上傳位址 (x-goog-upload-url)。
 *   2. 二進位內容傳輸 (Upload & Finalize)：傳輸圖片位元組並取回長效儲存參照識別碼 (Upload Reference ID)。
 */
class GeminiImageUploader(
    private val client: OkHttpClient,
    private val uploadUrl: String = GeminiConfig.UPLOAD_IMAGE_URL
) : ImageUploader {

    companion object {
        private const val TAG = "GeminiImageUploader"
        private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded;charset=UTF-8".toMediaType()
        private val BINARY_MEDIA_TYPE = "application/x-www-form-urlencoded;charset=utf-8".toMediaType()
    }

    override suspend fun uploadImages(
        images: List<ImagePayload>,
        cookies: Map<String, String>
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext emptyList()

        val cookiesHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val uploads = mutableListOf<Pair<String, String>>()

        for ((idx, img) in images.withIndex()) {
            val filename = img.filename.takeIf { it.isNotBlank() } ?: "image_${idx + 1}.jpg"
            val sizeBytes = img.data.size
            Log.i(TAG, "正在上傳第 ${idx + 1}/${images.size} 張圖片: '$filename' ($sizeBytes bytes)...")

            try {
                // Step 1: 初始化上傳會話 (Initiate Upload Session)
                val startBody = "File name: $filename".toRequestBody(FORM_MEDIA_TYPE)
                val startReqBuilder = Request.Builder()
                    .url(uploadUrl)
                    .post(startBody)

                if (cookiesHeader.isNotBlank()) {
                    startReqBuilder.header("Cookie", cookiesHeader)
                }

                GeminiConfig.UPLOAD_IMAGE_HEADERS.forEach { (k, v) ->
                    startReqBuilder.header(k, v)
                }
                startReqBuilder.header("x-goog-upload-command", "start")
                startReqBuilder.header("x-goog-upload-header-content-length", sizeBytes.toString())

                val uploadSessionUrl = client.newCall(startReqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        val bodySnippet = response.body?.string()?.take(200) ?: ""
                        throw NetworkException(
                            "初始化圖片上傳會話失敗 (HTTP ${response.code}): $bodySnippet"
                        )
                    }

                    // 遵循 RFC 9110 / HTTP/2 標頭大小寫不敏感規範
                    val sessionUrl = response.header("x-goog-upload-url")
                    if (sessionUrl.isNullOrBlank()) {
                        throw NetworkException(
                            "初始化圖片上傳回應中缺少 'x-goog-upload-url' 標頭 (HTTP ${response.code})"
                        )
                    }
                    sessionUrl
                }

                // Step 2: 傳輸二進位內容並確認完成 (Upload Binary Payload & Finalize)
                val uploadBody = img.data.toRequestBody(BINARY_MEDIA_TYPE)
                val finalizeReqBuilder = Request.Builder()
                    .url(uploadSessionUrl)
                    .post(uploadBody)

                if (cookiesHeader.isNotBlank()) {
                    finalizeReqBuilder.header("Cookie", cookiesHeader)
                }

                GeminiConfig.UPLOAD_IMAGE_HEADERS.forEach { (k, v) ->
                    finalizeReqBuilder.header(k, v)
                }
                finalizeReqBuilder.header("x-goog-upload-command", "upload, finalize")
                finalizeReqBuilder.header("x-goog-upload-offset", "0")

                val uploadRef = client.newCall(finalizeReqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        val bodySnippet = response.body?.string()?.take(200) ?: ""
                        throw NetworkException(
                            "完成圖片二進位傳輸失敗 (HTTP ${response.code}): $bodySnippet"
                        )
                    }

                    val ref = response.body?.string()?.trim() ?: ""
                    if (ref.isEmpty()) {
                        throw NetworkException("伺服器於完成階段回傳空白的圖片參照識別碼 (Upload Reference ID)")
                    }
                    ref
                }

                Log.i(TAG, "圖片 '$filename' 上傳成功 (ref: $uploadRef)")
                uploads.add(Pair(uploadRef, filename))

            } catch (e: Exception) {
                Log.e(TAG, "圖片 '$filename' 上傳失敗: ${e.message}", e)
                throw if (e is NetworkException) e else NetworkException("圖片上傳失敗 ('$filename'): ${e.message}", e)
            }
        }

        uploads
    }
}
