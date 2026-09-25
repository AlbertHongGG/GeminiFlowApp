package com.geminiflow.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * AppColors: Directly ported from LensWise (lib/core/theme/app_colors.dart)
 */
object AppColors {
    // Backgrounds
    val backgroundLight = Color(0xFFF5F7FA) // Soft Grey-White
    val backgroundDark = Color(0xFF0F111A)  // Deep Void Blue

    val surfaceLight = Color(0xFFFFFFFF)    // Pure White
    val surfaceDark = Color(0xFF1E2130)

    // Accents
    val primary = Color(0xFF475569)         // Muted Slate 600 (LensWise signature primary)
    val secondary = Color(0xFF94A3B8)       // Slate 400

    // Text
    val textPrimaryLight = Color(0xFF1A1D2B)
    val textSecondaryLight = Color(0xFF6E768C)

    val textPrimaryDark = Color(0xFFFFFFFF)
    val textSecondaryDark = Color(0xFFAAB2C8)

    // Semantic Accents
    val success = Color(0xFF10B981)         // Emerald
    val danger = Color(0xFFEF4444)          // Red Accent
    val warning = Color(0xFFF59E0B)         // Amber
    val borderLight = Color(0x0F000000)     // Black 6% subtle border
    val divider = Color(0x0D000000)         // Black 5% indented divider
}

// Aliases for unified consumption
val BgCanvas = AppColors.backgroundLight
val SurfaceCard = AppColors.surfaceLight
val SurfaceElevated = Color(0xFFF1F5F9)

val AccentPrimary = AppColors.primary
val AccentSecondary = AppColors.secondary
val AccentBlue = AppColors.primary
val AccentBlueLight = AppColors.primary.copy(alpha = 0.1f)
val AccentEmerald = AppColors.success
val AccentEmeraldLight = AppColors.success.copy(alpha = 0.1f)
val AccentRose = AppColors.danger
val AccentRoseLight = AppColors.danger.copy(alpha = 0.1f)
val AccentAmber = AppColors.warning
val AccentAmberLight = AppColors.warning.copy(alpha = 0.1f)

val BorderLight = AppColors.borderLight
val BorderFocused = AppColors.secondary

val TextPrimary = AppColors.textPrimaryLight
val TextSecondary = AppColors.textSecondaryLight
val TextMuted = AppColors.secondary

// Legacy compatibility
val BluePrimary = AccentPrimary
val BlueSecondary = AccentSecondary
val BlueTertiary = Color(0xFF60A5FA)
val GreenSuccess = AccentEmerald
val RedError = AccentRose
val OrangeWarning = AccentAmber
