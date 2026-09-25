package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.layout.Box
import com.geminiflow.app.presentation.notification.NotificationController

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.geminiflow.app.GeminiFlowApplication
import com.geminiflow.app.data.auth.browser.AuthBrowserConfig
import com.geminiflow.app.data.auth.browser.CookieBridge
import com.geminiflow.app.data.auth.browser.SecureAuthWebViewSetup
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authRepo = GeminiFlowApplication.instance.authRepository
    val cookieBridge = remember { CookieBridge() }

    var webViewInstance by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var useSafariUa by remember { mutableStateOf(false) }

    fun handleAuthSuccess(cookies: Map<String, String>) {
        val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        scope.launch {
            authRepo.saveCookies(cookieHeader)
            NotificationController.showSuccess("Google 帳號認證成功！已取得憑證")
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Google 帳號登入",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    actions = {
                    IconButton(
                        onClick = {
                            useSafariUa = !useSafariUa
                            webViewInstance?.let { wv ->
                                SecureAuthWebViewSetup.configure(
                                    webView = wv,
                                    useSafariUa = useSafariUa,
                                    onAuthSuccess = ::handleAuthSuccess,
                                    onProgress = { pageProgress = it / 100f }
                                )
                                wv.loadUrl(AuthBrowserConfig.INITIAL_LOGIN_URL)
                                val mode = if (useSafariUa) "Safari (iOS)" else "Chrome (Android)"
                                NotificationController.showInfo("已切換瀏覽器標識為：$mode")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = "切換 UA 核心", tint = TextSecondary)
                    }
                    IconButton(onClick = {
                        webViewInstance?.reload()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新整理", tint = TextSecondary)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            cookieBridge.clearAllCookies {
                                scope.launch {
                                    authRepo.clearAuth()
                                    webViewInstance?.loadUrl(AuthBrowserConfig.INITIAL_LOGIN_URL)
                                    NotificationController.showWarning("已清除目前登入狀態")
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "清除登入狀態", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCard,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (pageProgress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    color = AccentBlue,
                    trackColor = BorderLight,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            webViewInstance = this

                            SecureAuthWebViewSetup.configure(
                                webView = this,
                                useSafariUa = useSafariUa,
                                onAuthSuccess = ::handleAuthSuccess,
                                onProgress = { pageProgress = it / 100f }
                            )

                            loadUrl(AuthBrowserConfig.INITIAL_LOGIN_URL)
                        }
                    }
                )
            }
        }
    }
}
}
