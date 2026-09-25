package com.geminiflow.app.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 全域通知覆蓋層元件，掛載於最頂層，在有通知時自頂部流暢滑入展示。
 */
@Composable
fun GlobalNotificationOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val notifications by NotificationController.instance.notifications.collectAsState()
    val isDark = isSystemInDarkTheme()

    Box(modifier = modifier.fillMaxSize()) {
        // 主要畫面層
        content()

        // 浮動通知層
        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                notifications.forEach { notification ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 250)
                        ) + fadeOut(animationSpec = tween(250))
                    ) {
                        NotificationToast(
                            notification = notification,
                            onDismiss = {
                                NotificationController.instance.remove(notification.id)
                            },
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}
