package com.geminiflow.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 提供統一背景色與狀態列邊距的全螢幕容器元件。
 */
@Composable
fun ImmersiveScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.backgroundLight)
            .statusBarsPadding(),
        content = content
    )
}
