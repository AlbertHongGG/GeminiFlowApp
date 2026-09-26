package com.geminiflow.app.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自適應系統導航列安全間距組件。
 * 依據真機當前的導航列型態（3 鍵式 48~54dp、手勢小白條 16~24dp）動態計算實體高度，
 * 徹底解決硬編碼 Spacer 或 height(24.dp).navigationBarsPadding() 導致最後一個項目遭遮擋的根本問題。
 */
@Composable
fun NavigationBarsSafeSpacer(
    extraHeight: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalHeight = navBarHeight + extraHeight
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    )
}

/**
 * 動態計算包含導航列高度與額外呼吸空間的安全底部 Padding。
 * 適用於 LazyColumn 的 contentPadding = PaddingValues(..., bottom = navigationSafeBottomPadding(...))。
 */
@Composable
fun navigationSafeBottomPadding(extraPadding: Dp = 16.dp): Dp {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return navBarHeight + extraPadding
}
