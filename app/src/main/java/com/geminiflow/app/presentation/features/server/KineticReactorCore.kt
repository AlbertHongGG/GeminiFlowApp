package com.geminiflow.app.presentation.features.server

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 方案一：日冕一體化軌道鐘開關主體（Corona Orbit Switch Entity）
 * 擁有直徑 98dp 的高質感實體觸感陶瓷圓盤按鈕，背後環繞直徑達 184dp 的大氣層柔焦日冕湛藍光暈。
 * 徹底拋棄齒輪刻度與外圍小點，營造純淨無邊界的輕量美學。
 */
@Composable
fun KineticReactorCore(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "CoronaOrbitTransition")

    // 日冕光暈柔和呼吸膨脹動畫（2.4 秒平緩雙向）
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoronaBreathPulse"
    )

    // 實體按鍵下壓 Spring 物理彈性縮放（按壓時下沉至 0.94x 後彈性復位）
    val puckScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 550f),
        label = "PuckSpringScale"
    )

    // 核心圖示色調過渡
    val emblemColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF2563EB) else Color(0xFF334155),
        animationSpec = tween(durationMillis = 350),
        label = "EmblemColor"
    )

    Box(
        modifier = modifier.size(184.dp),
        contentAlignment = Alignment.Center
    ) {
        // 底層畫布：184dp 寬幅超柔焦大氣日冕光暈（Multi-pass Radial Aura）
        Box(
            modifier = Modifier
                .size(184.dp)
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)

                    // 外圍大氣光暈半徑（隨呼吸在 86dp ~ 94dp 間平緩起伏，直徑達 180dp+）
                    val outerRadius = 86.dp.toPx() + (8.dp.toPx() * pulseProgress)
                    val innerRimRadius = 66.dp.toPx()

                    // 第一層：廣域大氣擴散光暈（從底盤延伸至最外側）
                    val outerGlowBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF2563EB).copy(alpha = 0.85f * pulseProgress),
                            0.45f to Color(0xFF2563EB).copy(alpha = 0.72f * pulseProgress),
                            0.54f to Color(0xFF3B82F6).copy(alpha = 0.55f * pulseProgress),
                            0.70f to Color(0xFF60A5FA).copy(alpha = 0.32f * pulseProgress),
                            0.86f to Color(0xFF93C5FD).copy(alpha = 0.12f * pulseProgress),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = outerRadius
                    )

                    // 第二層：邊緣輝度增強光環（強化瓷白圓盤邊緣的透光立體度）
                    val rimGlowBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF3B82F6).copy(alpha = 0.35f * pulseProgress),
                            0.70f to Color(0xFF3B82F6).copy(alpha = 0.20f * pulseProgress),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = innerRimRadius
                    )

                    onDrawBehind {
                        if (isRunning) {
                            // 繪製廣域柔和日冕光暈
                            drawCircle(
                                brush = outerGlowBrush,
                                radius = outerRadius,
                                center = center
                            )
                            // 繪製圓盤邊緣輝度光圈
                            drawCircle(
                                brush = rimGlowBrush,
                                radius = innerRimRadius,
                                center = center
                            )
                        }
                    }
                }
        )

        // 上層：98dp 大尺度懸浮純白瓷質圓盤按鈕（具備實體 3D 浮空立體感）
        Surface(
            modifier = Modifier
                .size(98.dp)
                .scale(puckScale)
                .shadow(
                    elevation = if (isPressed) 2.dp else if (isRunning) 10.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = if (isRunning) Color(0x352563EB) else Color(0x180F172A),
                    ambientColor = Color(0x0C0F172A)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isRunning) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
            )
        ) {
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF8FAFC)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 運轉時按鈕內部微藍柔焦背光
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF).copy(alpha = pulseProgress))
                    )
                }

                // 核心電源微標（38dp 高清晰無襯線符號）
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isRunning) "停止本地服務" else "啟動本地伺服器",
                    tint = emblemColor,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}
