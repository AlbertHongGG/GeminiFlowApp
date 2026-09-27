package com.geminiflow.app.presentation.features.server

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 星鑽湛藍（Electric Azure）頂級亮色空間設計規範與色彩系統。
 * 嚴格杜絕綠色與笨重深黑，以純淨白、深板岩與科技湛藍營造現代純粹美學。
 */
object AzureTheme {
    // 空間基底
    val backgroundSnow = Color(0xFFF8FAFC)        // Slate 50
    val cardSurface = Color(0xFFFFFFFF)           // Pure White
    val slotBackground = Color(0xFFF1F5F9)        // Slate 100
    val slotBackgroundSubtle = Color(0xFFF8FAFC)  // Slate 50

    // 文字階層
    val textHeading = Color(0xFF0F172A)          // Slate 900
    val textBody = Color(0xFF334155)             // Slate 700
    val textMuted = Color(0xFF64748B)            // Slate 500
    val textDim = Color(0xFF94A3B8)              // Slate 400

    // 核心星鑽湛藍色系（0% 綠色）
    val azureDeep = Color(0xFF1D4ED8)            // Blue 700
    val azurePrimary = Color(0xFF2563EB)         // Blue 600
    val azureGlow = Color(0xFF3B82F6)            // Blue 500
    val azureLight = Color(0xFF60A5FA)           // Blue 400
    val azureSoft = Color(0xFFEFF6FF)            // Blue 50
    val azureBorder = Color(0xFFBFDBFE)          // Blue 200

    // 狀態色彩
    val statusStandby = Color(0xFF94A3B8)        // 冷灰待命
    val statusStandbySoft = Color(0xFFF1F5F9)
    val accentRose = Color(0xFFE11D48)           // 告警櫻紅
    val accentRoseSoft = Color(0xFFFFF1F2)

    // 邊框與分割線
    val borderSubtle = Color(0xFFE2E8F0)         // Slate 200
    val borderHairline = Color(0xFFEEF2F6)
    val borderActive = Color(0xFF93C5FD)         // Blue 300

    // 湛藍微光漸層邊界筆刷
    val activeGlowBrush = Brush.linearGradient(
        listOf(
            Color(0xFF3B82F6).copy(alpha = 0.6f),
            Color(0xFF60A5FA).copy(alpha = 0.3f),
            Color(0xFF3B82F6).copy(alpha = 0.5f)
        )
    )

    // 待命細緻邊界筆刷
    val subtleBorderBrush = Brush.linearGradient(
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFFE2E8F0)
        )
    )
}
