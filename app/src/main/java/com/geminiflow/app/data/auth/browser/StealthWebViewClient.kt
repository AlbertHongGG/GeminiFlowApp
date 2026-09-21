package com.geminiflow.app.data.auth.browser

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class StealthWebViewClient(
    private val cookieBridge: CookieBridge,
    private val onAuthSuccess: (Map<String, String>) -> Unit,
    private val onPageTitleChanged: ((String) -> Unit)? = null
) : WebViewClient() {

    companion object {
        private const val TAG = "StealthWebViewClient"
    }

    private var authCompleted = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        Log.d(TAG, "Navigating to: $url")

        checkAuthSession(url)
        return false // Keep inside WebView
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { checkAuthSession(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.title?.let { onPageTitleChanged?.invoke(it) }
        url?.let { checkAuthSession(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        Log.w(TAG, "Page load error: ${error?.description} on URL: ${request?.url}")
    }

    private fun checkAuthSession(url: String) {
        if (authCompleted) return

        if (url.contains(AuthBrowserConfig.GEMINI_DOMAIN) || url.contains("myaccount.google.com")) {
            cookieBridge.flush()
            val cookies = cookieBridge.getCookies(AuthBrowserConfig.GEMINI_APP_URL)
            if (!cookies[AuthBrowserConfig.REQUIRED_COOKIE_NAME].isNullOrBlank()) {
                Log.i(TAG, "Successfully captured Google auth cookies (__Secure-1PSID found)!")
                authCompleted = true
                onAuthSuccess(cookies)
            }
        }
    }

    fun reset() {
        authCompleted = false
    }
}
