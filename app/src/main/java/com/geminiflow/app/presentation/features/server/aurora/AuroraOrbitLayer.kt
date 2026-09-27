package com.geminiflow.app.presentation.features.server.aurora

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 雙軌道與發光衛星粒子系統（Dual Orbit Tracks & Floating Satellite Particles）
 * 1. 外軌道：220dp 虛線軌道，週期 30s 順時針旋轉，掛載 8dp 青色發光微星（#06B6D4）。
 * 2. 內軌道：160dp 細實線軌道，週期 20s 逆時針旋轉，掛載 6dp 洋紅發光微星（#EC4899）。
 * 3. 採用極座標幾何計算與 Canvas 徑向柔光漸層，以 GPU RenderNode 實現 0 重組流暢旋轉。
 */
@Composable
fun AuroraOrbitLayer(
    spec: AuroraOrbitSpec = AuroraVisualSpecDefaults.StandardOrbitSpec,
    profile: AuroraStateProfile,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitContinuousTransition")

    // 外軌道順時針連續旋轉（週期 30 秒）
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spec.outerTrackDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterOrbitRotation"
    )

    // 內軌道逆時針連續旋轉（週期 20 秒）
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spec.innerTrackDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerOrbitRotation"
    )

    // 平滑透明度過渡
    val animatedOrbitOpacity by animateFloatAsState(
        targetValue = profile.orbitOpacity,
        animationSpec = tween(durationMillis = 600),
        label = "OrbitOpacity"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Layer 3: 外層虛線軌道與青色發光衛星
        Canvas(
            modifier = Modifier
                .size(spec.outerTrackDiameter)
                .graphicsLayer {
                    rotationZ = outerRotation
                    alpha = animatedOrbitOpacity
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitRadius = (size.minDimension / 2f) - (spec.outerSatelliteDiameter.toPx() / 2f)

            // 繪製 1px 虛線軌道
            drawCircle(
                color = Color(0x3894A3B8),
                radius = orbitRadius,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(spec.outerTrackDashLength.toPx(), spec.outerTrackGapLength.toPx())
                    )
                )
            )

            // 頂部衛星座標（theta = -PI / 2）
            val satelliteCenter = Offset(
                x = center.x,
                y = center.y - orbitRadius
            )

            // 衛星外層柔光暈（Radial Gradient Glow）
            val glowRadiusPx = spec.outerSatelliteGlowRadius.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.outerSatelliteColor.copy(alpha = 0.65f),
                        spec.outerSatelliteColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = satelliteCenter,
                    radius = glowRadiusPx
                ),
                radius = glowRadiusPx,
                center = satelliteCenter
            )

            // 衛星核心實體
            drawCircle(
                color = spec.outerSatelliteColor,
                radius = spec.outerSatelliteDiameter.toPx() / 2f,
                center = satelliteCenter
            )
        }

        // Layer 3.5: 內層實線軌道與粉紅發光衛星
        Canvas(
            modifier = Modifier
                .size(spec.innerTrackDiameter)
                .graphicsLayer {
                    rotationZ = innerRotation
                    alpha = animatedOrbitOpacity * 0.75f
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitRadius = (size.minDimension / 2f) - (spec.innerSatelliteDiameter.toPx() / 2f)

            // 繪製 1px 細實線軌道
            drawCircle(
                color = Color(0x2694A3B8),
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 衛星座標（左下側 theta = 225 度）
            val angleRad = (225f * PI / 180f).toFloat()
            val satelliteCenter = Offset(
                x = center.x + orbitRadius * cos(angleRad),
                y = center.y + orbitRadius * sin(angleRad)
            )

            // 衛星外層柔光暈
            val glowRadiusPx = spec.innerSatelliteGlowRadius.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.innerSatelliteColor.copy(alpha = 0.65f),
                        spec.innerSatelliteColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = satelliteCenter,
                    radius = glowRadiusPx
                ),
                radius = glowRadiusPx,
                center = satelliteCenter
            )

            // 衛星核心實體
            drawCircle(
                color = spec.innerSatelliteColor,
                radius = spec.innerSatelliteDiameter.toPx() / 2f,
                center = satelliteCenter
            )
        }
    }
}
