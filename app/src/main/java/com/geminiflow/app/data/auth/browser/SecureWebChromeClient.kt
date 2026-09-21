package com.geminiflow.app.data.auth.browser

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class SecureWebChromeClient(
    private val onProgressChangedListener: (Int) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedListener(newProgress)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (view == null || resultMsg == null) return false

        val tempWebView = WebView(view.context).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                    val targetUrl = request?.url?.toString()
                    if (!targetUrl.isNullOrEmpty()) {
                        view.loadUrl(targetUrl)
                    }
                    return true
                }
            }
        }

        val transport = resultMsg.obj as? WebView.WebViewTransport
        transport?.webView = tempWebView
        resultMsg.sendToTarget()
        return true
    }
}
