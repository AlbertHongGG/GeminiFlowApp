package com.geminiflow.app.data.auth

import android.webkit.CookieManager
import com.geminiflow.app.data.api.GeminiConfig

class CookieManagerHelper {

    private val cookieManager: CookieManager
        get() = CookieManager.getInstance()

    fun getGoogleCookies(): Map<String, String> {
        val cookieString = cookieManager.getCookie(GeminiConfig.GEMINI_BASE_URL) ?: ""
        return parseCookieString(cookieString)
    }

    fun hasRequiredCookie(): Boolean {
        val cookies = getGoogleCookies()
        val pSid = cookies[GeminiConfig.REQUIRED_COOKIE_NAME]
        return !pSid.isNullOrBlank()
    }

    fun setCookies(rawCookies: String, domain: String = GeminiConfig.GEMINI_BASE_URL) {
        val items = rawCookies.split(";")
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty()) {
                cookieManager.setCookie(domain, trimmed)
            }
        }
        cookieManager.flush()
    }

    fun clearAllCookies() {
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    companion object {
        fun parseCookieString(cookieString: String): Map<String, String> {
            val map = mutableMapOf<String, String>()
            if (cookieString.isBlank()) return map

            val pairs = cookieString.split(";")
            for (pair in pairs) {
                val trimmed = pair.trim()
                val eqIdx = trimmed.indexOf('=')
                if (eqIdx > 0) {
                    val key = trimmed.substring(0, eqIdx).trim()
                    val value = trimmed.substring(eqIdx + 1).trim()
                    if (key.isNotEmpty()) {
                        map[key] = value
                    }
                }
            }
            return map
        }
    }
}
