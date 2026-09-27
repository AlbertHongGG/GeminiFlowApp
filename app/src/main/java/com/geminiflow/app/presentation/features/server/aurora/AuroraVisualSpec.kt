package com.geminiflow.app.presentation.features.server.aurora

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geminiflow.app.domain.model.server.ServerState

/**
 * 極光視覺規格資料模型（Aurora Visual Spec）
 * 嚴格遵循物件導向設計原則，將 GitChecker 視覺公式與數值完全物件化封裝。
 */
data class AuroraNebulaSpec(
    val primaryColors: List<Color>,
    val secondaryColors: List<Color>,
    val primarySize: Dp = 280.dp,
    val secondarySize: Dp = 220.dp,
    val primaryBlurRadius: Dp = 50.dp,
    val secondaryBlurRadius: Dp = 40.dp,
    val primaryDurationMs: Int = 20000,
    val secondaryDurationMs: Int = 25000
)

data class AuroraOrbitSpec(
    val outerTrackDiameter: Dp = 220.dp,
    val outerTrackDashLength: Dp = 6.dp,
    val outerTrackGapLength: Dp = 6.dp,
    val outerTrackDurationMs: Int = 30000,
    val outerSatelliteColor: Color = Color(0xFF06B6D4),
    val outerSatelliteDiameter: Dp = 8.dp,
    val outerSatelliteGlowRadius: Dp = 12.dp,

    val innerTrackDiameter: Dp = 160.dp,
    val innerTrackDurationMs: Int = 20000,
    val innerSatelliteColor: Color = Color(0xFFEC4899),
    val innerSatelliteDiameter: Dp = 6.dp,
    val innerSatelliteGlowRadius: Dp = 10.dp
)

/**
 * 伺服器狀態動態視覺輪廓（State Visual Profile）
 * 由 ServerState 領域模型驅動，決定動畫參數與光學物理屬性。
 */
data class AuroraStateProfile(
    val targetScale: Float,
    val targetOpacity: Float,
    val orbitOpacity: Float,
    val speedMultiplier: Float,
    val isPulsing: Boolean,
    val pulseDurationMs: Int,
    val pulseMinScale: Float,
    val pulseMaxScale: Float
)

object AuroraVisualSpecDefaults {

    // GitChecker 原版調色板
    val GitCheckerCyan = Color(0xFF06B6D4)
    val GitCheckerBlue = Color(0xFF3B82F6)
    val GitCheckerPurple = Color(0xFF8B5CF6)
    val GitCheckerPink = Color(0xFFEC4899)

    // Layer 1: 順時針流體漸層
    val DefaultPrimaryPalette = listOf(
        GitCheckerCyan,
        GitCheckerBlue,
        GitCheckerPurple,
        GitCheckerPink,
        GitCheckerCyan
    )

    // Layer 2: 逆時針反向流體漸層（互質光譜）
    val DefaultSecondaryPalette = listOf(
        GitCheckerPink,
        GitCheckerPurple,
        GitCheckerBlue,
        GitCheckerCyan,
        GitCheckerPink
    )

    // 異常故障告警專用光譜
    val AlertPrimaryPalette = listOf(
        Color(0xFFE11D48),
        Color(0xFFF43F5E),
        Color(0xFFFB7185),
        Color(0xFFBE123C),
        Color(0xFFE11D48)
    )

    val AlertSecondaryPalette = listOf(
        Color(0xFFBE123C),
        Color(0xFFFB7185),
        Color(0xFFF43F5E),
        Color(0xFFE11D48),
        Color(0xFFBE123C)
    )

    val StandardNebulaSpec = AuroraNebulaSpec(
        primaryColors = DefaultPrimaryPalette,
        secondaryColors = DefaultSecondaryPalette
    )

    val AlertNebulaSpec = AuroraNebulaSpec(
        primaryColors = AlertPrimaryPalette,
        secondaryColors = AlertSecondaryPalette
    )

    val StandardOrbitSpec = AuroraOrbitSpec()

    /**
     * 策略模式：根據 ServerState 取得精確的動態視覺輪廓
     */
    fun resolveProfile(serverState: ServerState): AuroraStateProfile {
        return when (serverState) {
            is ServerState.Stopped -> AuroraStateProfile(
                targetScale = 1.0f,
                targetOpacity = 0.14f,
                orbitOpacity = 0.10f,
                speedMultiplier = 0.6f,
                isPulsing = false,
                pulseDurationMs = 4000,
                pulseMinScale = 1.0f,
                pulseMaxScale = 1.0f
            )
            is ServerState.Starting -> AuroraStateProfile(
                targetScale = 1.10f,
                targetOpacity = 0.45f,
                orbitOpacity = 0.45f,
                speedMultiplier = 1.8f,
                isPulsing = true,
                pulseDurationMs = 1400,
                pulseMinScale = 1.03f,
                pulseMaxScale = 1.12f
            )
            is ServerState.Running -> AuroraStateProfile(
                targetScale = 1.05f,
                targetOpacity = 0.35f,
                orbitOpacity = 0.35f,
                speedMultiplier = 1.0f,
                isPulsing = true,
                pulseDurationMs = 3200,
                pulseMinScale = 1.01f,
                pulseMaxScale = 1.06f
            )
            is ServerState.Stopping -> AuroraStateProfile(
                targetScale = 1.0f,
                targetOpacity = 0.25f,
                orbitOpacity = 0.20f,
                speedMultiplier = 1.3f,
                isPulsing = true,
                pulseDurationMs = 1400,
                pulseMinScale = 0.98f,
                pulseMaxScale = 1.03f
            )
            is ServerState.Failed -> AuroraStateProfile(
                targetScale = 1.0f,
                targetOpacity = 0.35f,
                orbitOpacity = 0.30f,
                speedMultiplier = 0.8f,
                isPulsing = true,
                pulseDurationMs = 1500,
                pulseMinScale = 0.95f,
                pulseMaxScale = 1.05f
            )
        }
    }

    /**
     * 依據旋轉角度在調色板中平滑插值出即時極光光譜色
     * 讓中心圖示與極光星雲的旋轉光譜維持 100% 絕對同頻共振
     */
    fun evaluateAuroraColor(rotationDegrees: Float, palette: List<Color>): Color {
        if (palette.isEmpty()) return GitCheckerCyan
        if (palette.size == 1) return palette.first()

        val normalized = ((rotationDegrees % 360f) + 360f) % 360f
        val segmentCount = palette.size - 1
        val progress = normalized / 360f
        val scaled = progress * segmentCount
        val index = scaled.toInt().coerceIn(0, segmentCount - 1)
        val fraction = scaled - index

        val startColor = palette[index]
        val endColor = palette[index + 1]

        return androidx.compose.ui.graphics.lerp(startColor, endColor, fraction)
    }
}
