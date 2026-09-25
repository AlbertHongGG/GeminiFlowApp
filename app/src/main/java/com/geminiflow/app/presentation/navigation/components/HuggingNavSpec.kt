package com.geminiflow.app.presentation.navigation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 緊緻包裹正圓毛玻璃導航欄幾何與配色規格物件 (Apple VisionOS 晶透白瓷風格)。
 */
data class HuggingNavSpec(
    // 膠囊本體幾何規格 (緊緻包裹按鈕，四周勻稱對稱)
    val horizontalPadding: Dp = 8.dp,
    val verticalPadding: Dp = 6.dp,
    val itemSpacing: Dp = 14.dp,
    val containerCornerRadius: Dp = 32.dp,
    val elevation: Dp = 12.dp,

    // 晶透毛玻璃色彩 (70% 純白晶透無髒灰感)
    val blurRadius: Dp = 24.dp,
    val glassBackgroundColor: Color = Color.White.copy(alpha = 0.70f),
    val fallbackBackgroundColor: Color = Color.White.copy(alpha = 0.92f),
    val borderColor: Color = Color.Black.copy(alpha = 0.06f),
    val borderWidth: Dp = 1.dp,
    val ambientShadowColor: Color = Color.Black.copy(alpha = 0.05f),
    val spotShadowColor: Color = Color.Black.copy(alpha = 0.10f),

    // 圓形按鈕規格 (嚴格 CircleShape，直徑 50dp，圖標 24dp)
    val itemDiameter: Dp = 50.dp,
    val iconSize: Dp = 24.dp,

    // 方案 A 配色：Apple VisionOS 晶透白瓷浮雕圓盤 + 深邃石墨黑圖標
    val activeCircleColor: Color = Color.White,
    val activeCircleElevation: Dp = 4.dp,
    val activeCircleSpotShadow: Color = Color.Black.copy(alpha = 0.12f),
    val activeCircleAmbientShadow: Color = Color.Black.copy(alpha = 0.06f),
    val activeIconColor: Color = Color(0xFF0F172A), // 深邃石墨黑
    val inactiveIconColor: Color = Color(0xFF64748B), // Slate-500
    val inactiveCircleColor: Color = Color.Transparent,

    val animationDurationMillis: Int = 200
)
