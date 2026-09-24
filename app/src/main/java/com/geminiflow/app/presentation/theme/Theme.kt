package com.geminiflow.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CleanLightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = SurfaceCard,
    primaryContainer = AccentBlueLight,
    onPrimaryContainer = AccentBlue,
    
    secondary = TextSecondary,
    onSecondary = SurfaceCard,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    
    tertiary = AccentAmber,
    onTertiary = SurfaceCard,
    tertiaryContainer = AccentAmberLight,
    onTertiaryContainer = AccentAmber,
    
    background = BgCanvas,
    onBackground = TextPrimary,
    
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    outline = BorderLight,
    outlineVariant = BorderFocused,
    
    error = AccentRose,
    onError = SurfaceCard,
    errorContainer = AccentRoseLight,
    onErrorContainer = AccentRose
)

@Composable
fun GeminiFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanLightColorScheme,
        content = content
    )
}
