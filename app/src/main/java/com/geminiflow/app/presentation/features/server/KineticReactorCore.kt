package com.geminiflow.app.presentation.features.server

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 畫面視覺實體主體：動力反應核心（Kinetic Reactor Core）
 * 具備雕刻感凹槽基座、12 等分精密刻度軌道、三星光環公轉與觸覺瓷白彈簧浮雕按鈕。
 * 徹底揚棄單薄線條，作為整體主畫面的視覺主角。
 */
@Composable
fun KineticReactorCore(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "ReactorTransition")

    // 衛星軌道公轉角度（4.5 秒平滑旋轉 360 度）
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    // 核心微標呼吸光暈（1.8 秒雙向）
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // 按鍵下壓 Spring 彈性縮放（下壓 0.92x 後迅速回彈）
    val puckScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "PuckSpringScale"
    )

    val coreEmblemColor by animateColorAsState(
        targetValue = if (isRunning) AzureTheme.azurePrimary else AzureTheme.statusStandby,
        animationSpec = tween(durationMillis = 350),
        label = "EmblemColor"
    )

    val outerSocketColor = AzureTheme.slotBackground
    val trackRingColor = if (isRunning) AzureTheme.azureBorder else AzureTheme.borderSubtle
    val satelliteColor = if (isRunning) AzureTheme.azurePrimary else AzureTheme.textDim
    val tickMarkColor = if (isRunning) AzureTheme.azureLight.copy(alpha = 0.7f) else AzureTheme.borderSubtle

    Box(
        modifier = modifier
            .size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        // 底層畫布：凹槽基座 + 12 等分精密刻度軌道 + 三重衛星公轉（120fps 滿幀）
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(outerSocketColor)
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)
                    val trackRadius = 38.dp.toPx()
                    val tickLength = 3.5.dp.toPx()

                    onDrawBehind {
                        // 1. 內凹槽細微邊界與同心導軌
                        drawCircle(
                            color = trackRingColor,
                            radius = trackRadius,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // 2. 12 等分精密刻度線（如計時腕錶外圈刻度規）
                        for (i in 0 until 12) {
                            val angleRad = (i * 30.0 * PI / 180.0).toFloat()
                            val innerR = trackRadius - (tickLength / 2f)
                            val outerR = trackRadius + (tickLength / 2f)

                            val startX = center.x + innerR * cos(angleRad)
                            val startY = center.y + innerR * sin(angleRad)
                            val endX = center.x + outerR * cos(angleRad)
                            val endY = center.y + outerR * sin(angleRad)

                            drawLine(
                                color = tickMarkColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // 3. 三重衛星節點公轉（120 度等距分佈）
                        val baseAngle = if (isRunning) orbitAngle else 0f
                        for (j in 0..2) {
                            val nodeAngleRad = ((baseAngle + j * 120.0) * PI / 180.0).toFloat()
                            val nodeX = center.x + trackRadius * cos(nodeAngleRad)
                            val nodeY = center.y + trackRadius * sin(nodeAngleRad)

                            if (isRunning) {
                                // 運轉時微光光尾（拖曳動能）
                                val trailAngleRad = ((baseAngle + j * 120.0 - 12.0) * PI / 180.0).toFloat()
                                val trailX = center.x + trackRadius * cos(trailAngleRad)
                                val trailY = center.y + trackRadius * sin(trailAngleRad)
                                drawCircle(
                                    color = satelliteColor.copy(alpha = 0.35f),
                                    radius = 2.2.dp.toPx(),
                                    center = Offset(trailX, trailY)
                                )

                                // 主衛星節點外光環
                                drawCircle(
                                    color = satelliteColor.copy(alpha = 0.25f),
                                    radius = 5.dp.toPx(),
                                    center = Offset(nodeX, nodeY)
                                )
                            }

                            // 實心微衛星光球
                            drawCircle(
                                color = satelliteColor,
                                radius = 3.2.dp.toPx(),
                                center = Offset(nodeX, nodeY)
                            )
                        }
                    }
                }
        )

        // 上層：實體瓷白彈簧浮雕按鈕（Tactile Porcelain Puck）
        Surface(
            modifier = Modifier
                .size(54.dp)
                .scale(puckScale)
                .shadow(
                    elevation = if (isPressed) 1.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = Color(0x180F172A),
                    ambientColor = Color(0x0A0F172A)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            shape = CircleShape,
            color = AzureTheme.cardSurface,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isRunning) AzureTheme.azureBorder else AzureTheme.borderSubtle
            )
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                // 運作時背後微弱放射呼吸光
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AzureTheme.azureSoft.copy(alpha = pulseAlpha))
                    )
                }

                // 核心動力微標
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isRunning) "關閉伺服器" else "啟動伺服器",
                    tint = coreEmblemColor.copy(alpha = if (isRunning) pulseAlpha else 1.0f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
