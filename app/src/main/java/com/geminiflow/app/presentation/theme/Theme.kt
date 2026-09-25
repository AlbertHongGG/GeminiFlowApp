package com.geminiflow.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GeminiFlowLightColorScheme = lightColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.surfaceLight,
    primaryContainer = AppColors.primary.copy(alpha = 0.1f),
    onPrimaryContainer = AppColors.primary,

    secondary = AppColors.secondary,
    onSecondary = AppColors.surfaceLight,
    secondaryContainer = AppColors.backgroundLight,
    onSecondaryContainer = AppColors.textPrimaryLight,

    tertiary = AppColors.warning,
    onTertiary = AppColors.surfaceLight,

    background = AppColors.backgroundLight,
    onBackground = AppColors.textPrimaryLight,

    surface = AppColors.surfaceLight,
    onSurface = AppColors.textPrimaryLight,
    surfaceVariant = AppColors.backgroundLight,
    onSurfaceVariant = AppColors.textSecondaryLight,

    outline = AppColors.borderLight,
    outlineVariant = AppColors.secondary.copy(alpha = 0.3f),

    error = AppColors.danger,
    onError = AppColors.surfaceLight
)

@Composable
fun GeminiFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GeminiFlowLightColorScheme,
        content = content
    )
}
