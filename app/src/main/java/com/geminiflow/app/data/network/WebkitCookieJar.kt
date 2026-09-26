package com.geminiflow.app.data.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 遵循 RFC 6265 之動態 WebKit Cookie 管理器。
 * 委託 Android 原生 CookieManager 依據請求的目標 URL 動態解析並注入符合網域的憑證。
 *
 * 核心安全特性：
 * 1. 當請求 Hop 0 (lh3.googleusercontent.com) 時，因網域隔離不送 .google.com Cookie，確保取得 HTTP 302 跳轉。
 * 2. 當跟隨跳轉至 Hop 1 (*.google.com 跳板) 時，自動匹配並附帶 Google 身分憑證以通過驗證。
 * 3. 支援單元測試注入，提升可測試性與長期維護性。
 */
class WebkitCookieJar(
    private val cookieProvider: (url: String) -> String? = { url ->
        try {
            CookieManager.getInstance().getCookie(url)
        } catch (_: Exception) {
            null
        }
    },
    private val cookieSaver: ((url: String, cookieString: String) -> Unit)? = { url, cookieString ->
        try {
            CookieManager.getInstance().setCookie(url, cookieString)
        } catch (_: Exception) {
        }
    }
) : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieHeader = cookieProvider(url.toString()) ?: return emptyList()
        if (cookieHeader.isBlank()) return emptyList()

        val result = mutableListOf<Cookie>()
        val pairs = cookieHeader.split(";")
        for (pair in pairs) {
            val trimmed = pair.trim()
            if (trimmed.isEmpty()) continue
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val name = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                if (name.isNotEmpty()) {
                    val cookie = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(url.host)
                        .path("/")
                        .build()
                    result.add(cookie)
                }
            }
        }
        return result
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookieSaver == null || cookies.isEmpty()) return
        val urlString = url.toString()
        for (cookie in cookies) {
            cookieSaver.invoke(urlString, cookie.toString())
        }
    }
}
