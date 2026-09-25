package com.geminiflow.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 頁面頂部配置標題區塊，包含主標題、副標題與背景裝飾 ThinkingOrb。
 */
@Composable
fun PremiumConfigHeader(
    title: String = "進階設定",
    subtitle: String = "SYSTEM CONFIGURATION",
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 38.dp, bottom = 24.dp)
    ) {
        // 裝飾用 ThinkingOrb：設定佈局尺寸為 (0, 0)，避免影響父容器高度計算
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, minHeight = 0)
                    )
                    layout(0, 0) {
                        placeable.placeRelative(
                            -placeable.width + 16.dp.roundToPx(),
                            -placeable.height / 2
                        )
                    }
                }
                .alpha(0.20f)
        ) {
            ThinkingOrb(size = 150.dp, isDark = false)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.textPrimaryLight,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textSecondaryLight,
                    letterSpacing = 1.5.sp
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailing()
            }
        }
    }
}
