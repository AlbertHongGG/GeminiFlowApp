package com.geminiflow.app.data.network.export

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.ImageDownloadException
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import com.geminiflow.app.domain.repository.ImageExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.random.Random

/**
 * Google Gemini 官方網頁版 c8o8Fe 原圖導出 RPC 實作。
 * 負責發送 2816x1536 超高解析度無損原圖導出請求，絕無降級或靜默妥協。
 */
class GeminiImageExportService(
    private val client: OkHttpClient,
    private val payloadBuilder: ImageExportPayloadBuilder = ImageExportPayloadBuilder(),
    private val responseParser: ImageExportResponseParser = ImageExportResponseParser()
) : ImageExportService {

    companion object {
        private const val TAG = "GeminiImageExport"
        private const val RPC_ID = ImageExportPayloadBuilder.RPC_ID
    }

    override suspend fun exportHighResolutionImageUrl(
        metadata: ImageExportMetadata,
        tokens: GeminiTokens,
        cookies: Map<String, String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanConvId = metadata.conversationId.removePrefix("c_")
        val sourcePath = if (cleanConvId.isNotEmpty()) "/app/$cleanConvId" else "/app"
        val cookiesHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val builtPayload = payloadBuilder.build(metadata)

        val urlBuilder = "https://gemini.google.com/_/BardChatUi/data/batchexecute".toHttpUrlOrNull()
            ?.newBuilder()
            ?: return@withContext Result.failure(IllegalStateException("無效的 batchexecute 基礎網址"))

        urlBuilder.addQueryParameter("rpcids", RPC_ID)
        urlBuilder.addQueryParameter("source-path", sourcePath)
        urlBuilder.addQueryParameter("bl", GeminiConfig.REQUEST_BL_PARAM)
        if (!tokens.sid.isNullOrBlank()) {
            urlBuilder.addQueryParameter("f.sid", tokens.sid)
        }
        urlBuilder.addQueryParameter("hl", "zh-TW")
        urlBuilder.addQueryParameter("_reqid", Random.nextInt(100000, 999999).toString())
        urlBuilder.addQueryParameter("rt", "c")

        val formBody = FormBody.Builder()
            .add("at", tokens.snlm0e)
            .add("f.req", builtPayload.outerJson)
            .build()

        val request = Request.Builder()
            .url(urlBuilder.build())
            .post(formBody)
            .header("Cookie", cookiesHeader)
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .header("X-Same-Domain", "1")
            .header("Origin", "https://gemini.google.com")
            .header("Referer", "https://gemini.google.com/")
            .header("User-Agent", GeminiConfig.DEFAULT_USER_AGENT)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext Result.failure(
                        ImageDownloadException("Google c8o8Fe 原圖導出請求失敗 (HTTP $code)")
                    )
                }
                val body = response.body?.string() ?: ""
                val highResUrl = responseParser.parse(body)
                Log.i(TAG, "成功取得 2816x1536 超高解析原圖網址: $highResUrl (追蹤碼: ${builtPayload.correlationId})")
                Result.success(highResUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "執行 c8o8Fe 原圖導出異常: ${e.message}", e)
            Result.failure(e)
        }
    }
}
