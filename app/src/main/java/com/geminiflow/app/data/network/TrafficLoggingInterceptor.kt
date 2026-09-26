package com.geminiflow.app.data.network

import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.log.TrafficLog
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 全域 OkHttp 網路流量日誌攔截器。
 * 負責自動截獲應用程式內部所有發往 Gemini API、圖片下載、身分驗證的請求與回應，
 * 透過 Request.tag(RequestTelemetryTag::class.java) 提取記憶體遙測標籤，
 * 100% 遵守 RFC 7230 協定標準，完全相容任何 Unicode/繁體中文字元。
 */
class TrafficLoggingInterceptor(
    private val trafficLogManager: TrafficLogManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val path = url.encodedPath
        val method = request.method
        val t0 = System.currentTimeMillis()

        // 從記憶體中安全取得強型別遙測標籤，絕不讀寫 HTTP Wire Header
        val tag = request.tag(RequestTelemetryTag::class.java)
        val promptSummary = tag?.promptSummary
        val source = tag?.source ?: "API Client"
        val displayPath = when {
            tag != null -> "/stream ($source)"
            path.contains("batchexecute") -> "/stream ($source)"
            path.contains("upload") -> "/upload/image"
            path.contains("image") || path.contains("gg-dl") -> "/images/download"
            else -> path
        }

        try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - t0
            val code = response.code

            trafficLogManager.record(
                TrafficLog(
                    method = method,
                    path = displayPath,
                    statusCode = code,
                    durationMs = duration,
                    clientIp = "127.0.0.1",
                    promptSummary = promptSummary,
                    responseSummary = if (response.isSuccessful) "HTTP $code OK" else "HTTP $code Error",
                    isError = !response.isSuccessful
                )
            )
            return response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - t0
            trafficLogManager.record(
                TrafficLog(
                    method = method,
                    path = displayPath,
                    statusCode = 500,
                    durationMs = duration,
                    clientIp = "127.0.0.1",
                    promptSummary = promptSummary,
                    responseSummary = "連線失敗: ${e.message ?: e.javaClass.simpleName}",
                    isError = true
                )
            )
            throw e
        }
    }
}
