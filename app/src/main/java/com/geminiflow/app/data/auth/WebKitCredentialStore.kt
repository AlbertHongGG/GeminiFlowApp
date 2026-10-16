package com.geminiflow.app.data.auth

import android.webkit.CookieManager
import com.geminiflow.app.data.api.GeminiConfig

/**
 * 基於 Android WebKit 原生 CookieManager 的憑證儲存實作。
 * 遵循單一事實來源原則，精準整合 /app 與根路徑之 Session Cookies。
 */
class WebKitCredentialStore(
    private val cookieGetter: (url: String) -> String? = { url ->
        try {
            CookieManager.getInstance().getCookie(url)
        } catch (_: Exception) {
            null
        }
    },
    private val cookieSetter: ((url: String, cookie: String) -> Unit)? = { url, cookie ->
        try {
            CookieManager.getInstance().setCookie(url, cookie)
            CookieManager.getInstance().flush()
        } catch (_: Exception) {
        }
    },
    private val cookieClearer: (() -> Unit)? = {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (_: Exception) {
        }
    }
) : CredentialStore {

    override fun getCookies(): Map<String, String> {
        val appCookies = cookieGetter(GeminiConfig.GEMINI_APP_URL) ?: ""
        val baseCookies = cookieGetter(GeminiConfig.GEMINI_BASE_URL) ?: ""
        val map = CookieManagerHelper.parseCookieString(baseCookies).toMutableMap()
        map.putAll(CookieManagerHelper.parseCookieString(appCookies))
        return map
    }

    override fun hasRequiredCredential(): Boolean {
        val cookies = getCookies()
        val pSid = cookies[GeminiConfig.REQUIRED_COOKIE_NAME]
        return !pSid.isNullOrBlank()
    }

    override fun saveCookies(cookiesHeader: String) {
        if (cookieSetter == null) return
        val items = cookiesHeader.split(";")
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty()) {
                cookieSetter.invoke(GeminiConfig.GEMINI_APP_URL, trimmed)
                cookieSetter.invoke(GeminiConfig.GEMINI_BASE_URL, trimmed)
            }
        }
    }

    override fun clearCredentials() {
        cookieClearer?.invoke()
    }
}
