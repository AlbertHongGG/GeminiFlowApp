package com.geminiflow.app.data.auth.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

object SecureAuthWebViewSetup {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        webView: WebView,
        useSafariUa: Boolean = false,
        onAuthSuccess: (Map<String, String>) -> Unit,
        onProgress: (Int) -> Unit
    ): StealthWebViewClient {
        val cookieBridge = CookieBridge()
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        val settings = webView.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true

            // Set clean, un-flagged Mobile User-Agent
            userAgentString = if (useSafariUa) {
                AuthBrowserConfig.SAFARI_IOS_UA
            } else {
                AuthBrowserConfig.buildCleanMobileChromeUa(userAgentString)
            }
        }

        // Suppress X-Requested-With header to avoid embedded WebView detection
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            try {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
            } catch (_: Exception) {
            }
        }

        val stealthClient = StealthWebViewClient(
            cookieBridge = cookieBridge,
            onAuthSuccess = onAuthSuccess
        )
        webView.webViewClient = stealthClient
        webView.webChromeClient = SecureWebChromeClient(onProgress)

        return stealthClient
    }
}
