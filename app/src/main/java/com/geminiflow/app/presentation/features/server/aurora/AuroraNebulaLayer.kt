package com.geminiflow.app.presentation.features.server.aurora

import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * 雙層反向極光星雲流體層（Pure Circular Skia Shader Aurora Nebula）
 * 1. 徹底消滅 RenderEffect.blur 所造成的方形邊界裁剪問題。
 * 2. 採用 Skia 原生 GPU ComposeShader：
 *    - SweepGradient：承載順時針與逆時針 360 度旋轉之五色光譜。
 *    - RadialGradient：以高斯鐘型衰減函數（Gaussian Falloff）嚴格於極座標圓周半徑處平滑漸隱至 0。
 *    - PorterDuff.Mode.DST_IN：二者於 GPU 像素著色器中相乘，保證 100% 絕對純圓漫射，無任何稜角。
 * 3. 雙層旋轉週期互質（20s 與 25s），產生永不重複的流體物理光譜干涉。
 */
@Composable
fun AuroraNebulaLayer(
    spec: AuroraNebulaSpec = AuroraVisualSpecDefaults.StandardNebulaSpec,
    profile: AuroraStateProfile,
    primaryRotation: Float,
    secondaryRotation: Float,
    modifier: Modifier = Modifier
) {
    // 預分配 IntArray 色彩與位置，避免每幀重分配 GC 抖動
    val primaryIntColors = remember(spec.primaryColors) {
        spec.primaryColors.map { it.toArgb() }.toIntArray()
    }
    val primaryPositions = remember(spec.primaryColors) {
        val count = spec.primaryColors.size
        FloatArray(count) { i -> i.toFloat() / (count - 1).toFloat() }
    }

    val secondaryIntColors = remember(spec.secondaryColors) {
        spec.secondaryColors.map { it.toArgb() }.toIntArray()
    }
    val secondaryPositions = remember(spec.secondaryColors) {
        val count = spec.secondaryColors.size
        FloatArray(count) { i -> i.toFloat() / (count - 1).toFloat() }
    }

    // 輕盈柔和的大氣高斯衰減（Airy Gaussian Decay Mask）：降低核心濃度，消除過度飽和實體感
    val radialMaskColors = remember {
        intArrayOf(
            android.graphics.Color.argb(165, 255, 255, 255),
            android.graphics.Color.argb(150, 255, 255, 255),
            android.graphics.Color.argb(110, 255, 255, 255),
            android.graphics.Color.argb(65, 255, 255, 255),
            android.graphics.Color.argb(25, 255, 255, 255),
            android.graphics.Color.argb(6, 255, 255, 255),
            android.graphics.Color.argb(0, 255, 255, 255)
        )
    }
    val radialMaskPositions = remember {
        floatArrayOf(0.0f, 0.25f, 0.45f, 0.65f, 0.82f, 0.94f, 1.0f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "NebulaPulseTransition")

    // 呼吸動態：由當前 State Profile 決定
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = profile.pulseMinScale,
        targetValue = profile.pulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = profile.pulseDurationMs,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NebulaPulse"
    )

    // 平滑過渡目標縮放與透明度
    val animatedBaseScale by animateFloatAsState(
        targetValue = profile.targetScale,
        animationSpec = tween(durationMillis = 600),
        label = "NebulaBaseScale"
    )

    val animatedOpacity by animateFloatAsState(
        targetValue = profile.targetOpacity,
        animationSpec = tween(durationMillis = 600),
        label = "NebulaOpacity"
    )

    val compositeScale = if (profile.isPulsing) animatedBaseScale * pulseProgress else animatedBaseScale

    // 預分配 Paint 與 Matrix 物件以達 0 GC 抖動
    val primaryPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    }
    val secondaryPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    }
    val rotationMatrix = remember { Matrix() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(spec.primarySize)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val primaryRadiusPx = (spec.primarySize.toPx() / 2f) * compositeScale
            val secondaryRadiusPx = (spec.secondarySize.toPx() / 2f) * compositeScale

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas

                // 1. Layer 1：大星雲（順時針旋轉錐形漸層 x 高斯徑向衰減遮罩）
                if (animatedOpacity > 0.01f && primaryRadiusPx > 0.1f) {
                    val primarySweep = SweepGradient(
                        center.x, center.y,
                        primaryIntColors,
                        primaryPositions
                    )
                    rotationMatrix.setRotate(primaryRotation, center.x, center.y)
                    primarySweep.setLocalMatrix(rotationMatrix)

                    val primaryRadial = RadialGradient(
                        center.x, center.y,
                        primaryRadiusPx,
                        radialMaskColors,
                        radialMaskPositions,
                        Shader.TileMode.CLAMP
                    )

                    primaryPaint.shader = ComposeShader(
                        primarySweep,
                        primaryRadial,
                        PorterDuff.Mode.DST_IN
                    )
                    primaryPaint.alpha = (animatedOpacity * 255f).toInt().coerceIn(0, 255)

                    native.drawCircle(center.x, center.y, primaryRadiusPx, primaryPaint)
                }

                // 2. Layer 2：反向小星雲（逆時針旋轉錐形漸層 x 高斯徑向衰減遮罩）
                if (animatedOpacity > 0.01f && secondaryRadiusPx > 0.1f) {
                    val secondarySweep = SweepGradient(
                        center.x, center.y,
                        secondaryIntColors,
                        secondaryPositions
                    )
                    rotationMatrix.setRotate(secondaryRotation, center.x, center.y)
                    secondarySweep.setLocalMatrix(rotationMatrix)

                    val secondaryRadial = RadialGradient(
                        center.x, center.y,
                        secondaryRadiusPx,
                        radialMaskColors,
                        radialMaskPositions,
                        Shader.TileMode.CLAMP
                    )

                    secondaryPaint.shader = ComposeShader(
                        secondarySweep,
                        secondaryRadial,
                        PorterDuff.Mode.DST_IN
                    )
                    secondaryPaint.alpha = (animatedOpacity * 0.50f * 255f).toInt().coerceIn(0, 255)

                    native.drawCircle(center.x, center.y, secondaryRadiusPx, secondaryPaint)
                }
            }
        }
    }
}
