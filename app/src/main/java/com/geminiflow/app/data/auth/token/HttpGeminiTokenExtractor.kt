package com.geminiflow.app.data.auth.token

import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.TokenExtractionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

/**
 * 透過 HTTP GET 向 https://gemini.google.com/app 抽取權杖之實作。
 * 採用標準擬真瀏覽器導航 Headers，防止被 Google 邊緣防火牆阻擋。
 */
class HttpGeminiTokenExtractor(
    private val client: OkHttpClient
) : TokenExtractor {

    companion object {
        private const val TAG = "HttpGeminiTokenExtractor"

        private val SNLM0E_PATTERNS = listOf(
            Pattern.compile("""SNlM0e\\":\s*\\"(.*?)\\""""),
            Pattern.compile("""SNlM0e"\s*:\s*"(.*?)""""),
            Pattern.compile("""SNlM0e['"]?\s*[:=]\s*['"]?([^'",\s}]+)""")
        )
        private val SID_PATTERNS = listOf(
            Pattern.compile(""""FdrFJe"\s*:\s*"([\d-]+)""""),
            Pattern.compile("""FdrFJe\\":\s*\\"([\d-]+)\\""""),
            Pattern.compile("""FdrFJe['"]?\s*[:=]\s*['"]?([\d-]+)""")
        )
        private val BL_PATTERNS = listOf(
            Pattern.compile(""""cfb2h"\s*:\s*"([^"]+)""""),
            Pattern.compile("""cfb2h\\":\s*\\"([^\\"]+)\\""""),
            Pattern.compile("""(boq_gemini-web-uiserver_[0-9a-zA-Z._]+)""")
        )
    }

    override suspend fun extractTokens(cookies: Map<String, String>): GeminiTokens = withContext(Dispatchers.IO) {
        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val reqBuilder = Request.Builder()
            .url(GeminiConfig.GEMINI_APP_URL)
            .header("Cookie", cookieHeader)

        GeminiConfig.BROWSER_NAVIGATION_HEADERS.forEach { (k, v) ->
            reqBuilder.header(k, v)
        }

        val request = reqBuilder.get().build()

        val html = try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                throw TokenExtractionException("HTTP 請求 Gemini 首頁失敗 (HTTP $code)")
            }
            val body = response.body?.string() ?: ""
            response.close()
            body
        } catch (e: TokenExtractionException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Gemini home page: ${e.message}", e)
            throw TokenExtractionException("連線至 Gemini 首頁失敗: ${e.message}", e)
        }

        parseHtml(html)
    }

    fun parseHtml(html: String): GeminiTokens {
        var snlm0e: String? = null
        for (pattern in SNLM0E_PATTERNS) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    snlm0e = candidate
                    break
                }
            }
        }

        if (snlm0e.isNullOrBlank()) {
            throw TokenExtractionException("無法從 Gemini 網頁中解析授權權杖 (SNlM0e)")
        }

        var sid: String? = null
        for (pattern in SID_PATTERNS) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    sid = candidate
                    break
                }
            }
        }

        var serverBl: String? = null
        for (pattern in BL_PATTERNS) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    serverBl = candidate
                    break
                }
            }
        }

        return GeminiTokens(snlm0e = snlm0e, sid = sid, serverBl = serverBl)
    }
}
