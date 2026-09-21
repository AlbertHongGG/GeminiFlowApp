package com.geminiflow.app.data.auth.browser

import android.webkit.CookieManager
import com.geminiflow.app.data.auth.CookieManagerHelper

class CookieBridge {

    private val cookieManager: CookieManager
        get() = CookieManager.getInstance()

    fun getCookies(url: String = AuthBrowserConfig.GEMINI_APP_URL): Map<String, String> {
        val raw = cookieManager.getCookie(url) ?: ""
        return CookieManagerHelper.parseCookieString(raw)
    }

    fun hasValidAuthSession(): Boolean {
        val cookies = getCookies(AuthBrowserConfig.GEMINI_APP_URL)
        val psid = cookies[AuthBrowserConfig.REQUIRED_COOKIE_NAME]
        return !psid.isNullOrBlank()
    }

    fun flush() {
        cookieManager.flush()
    }

    fun clearAllCookies(onCleared: (() -> Unit)? = null) {
        cookieManager.removeAllCookies {
            cookieManager.flush()
            onCleared?.invoke()
        }
    }
}
