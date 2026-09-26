package com.geminiflow.app.data.repository

import android.content.Context
import android.util.Log
import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class AuthRepositoryImpl(
    private val context: Context,
    private val client: OkHttpClient,
    private val cookieHelper: CookieManagerHelper
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private val SNLM0E_PATTERN_1 = Pattern.compile("""SNlM0e\\":\\"(.*?)\\"""")
        private val SNLM0E_PATTERN_2 = Pattern.compile("""SNlM0e":"(.*?)"""")
        private val SID_PATTERN = Pattern.compile(""""FdrFJe":"([\d-]+)"""")
    }

    private val _isAuthenticated = MutableStateFlow(cookieHelper.hasRequiredCookie())
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var cachedTokens: GeminiTokens? = null

    override suspend fun getGoogleCookies(): Map<String, String> {
        return cookieHelper.getGoogleCookies()
    }

    override suspend fun saveCookies(cookiesHeader: String) {
        cookieHelper.setCookies(cookiesHeader)
        _isAuthenticated.value = cookieHelper.hasRequiredCookie()
        cachedTokens = null
    }

    override suspend fun clearAuth() {
        cookieHelper.clearAllCookies()
        cachedTokens = null
        _isAuthenticated.value = false
    }

    override suspend fun ensureValidTokens(forceRefresh: Boolean): GeminiTokens = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTokens != null) {
            return@withContext cachedTokens!!
        }

        val cookies = cookieHelper.getGoogleCookies()
        val pSid = cookies[GeminiConfig.REQUIRED_COOKIE_NAME]
        if (pSid.isNullOrBlank()) {
            _isAuthenticated.value = false
            throw AuthenticationRequiredException("缺少必要的 Google 驗證憑證 (${GeminiConfig.REQUIRED_COOKIE_NAME})，請在 App 內登入 Google 帳號。")
        }

        val tokens = fetchTokensViaHttp(cookies)
        if (tokens != null) {
            cachedTokens = tokens
            _isAuthenticated.value = true
            return@withContext tokens
        }

        _isAuthenticated.value = false
        throw AuthenticationRequiredException("無法從 Gemini 首頁解析授權權杖 (SNlM0e)，您的登入可能已過期或被安全驗證阻擋，請重新登入。")
    }

    private fun fetchTokensViaHttp(cookies: Map<String, String>): GeminiTokens? {
        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val request = Request.Builder()
            .url(GeminiConfig.GEMINI_BASE_URL)
            .header("User-Agent", GeminiConfig.DEFAULT_USER_AGENT)
            .header("Cookie", cookieHeader)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP fetch tokens returned status: ${response.code}")
                response.close()
                return null
            }

            val html = response.body?.string() ?: ""
            response.close()
            extractTokensFromHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tokens via HTTP: ${e.message}", e)
            null
        }
    }

    fun extractTokensFromHtml(html: String): GeminiTokens? {
        var snlm0e: String? = null
        var matcher = SNLM0E_PATTERN_1.matcher(html)
        if (matcher.find()) {
            snlm0e = matcher.group(1)
        } else {
            matcher = SNLM0E_PATTERN_2.matcher(html)
            if (matcher.find()) {
                snlm0e = matcher.group(1)
            }
        }

        if (snlm0e.isNullOrBlank()) {
            return null
        }

        var sid: String? = null
        val sidMatcher = SID_PATTERN.matcher(html)
        if (sidMatcher.find()) {
            sid = sidMatcher.group(1)
        }

        return GeminiTokens(snlm0e = snlm0e, sid = sid)
    }
}
