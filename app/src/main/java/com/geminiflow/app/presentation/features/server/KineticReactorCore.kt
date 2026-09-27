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
import com.geminiflow.app.domain.model.server.ServerState

/**
 * 柔焦大氣日冕環開關主體（Atmospheric Corona Glow Core）
 * 1. 直接由 serverState: ServerState 強型別驅動，徹底消滅布林盲區。
 * 2. 狀態精確對應：
 *    - Starting: 能量凝聚加速呼吸（1200ms），按鍵鎖定。
 *    - Running: 沉靜大氣深層呼吸（3000ms），光暈漫射。
 *    - Stopping: 能量平緩收縮（1200ms），按鍵鎖定。
 *    - Stopped: 乾淨瓷白，無光暈。
 *    - Failed: 櫻紅故障警示。
 */
@Composable
fun KineticReactorCore(
    serverState: ServerState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = serverState.isRunning
    val isTransitioning = serverState.isTransitioning

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "CoronaOrbitTransition")

    // 日冕光暈呼吸動畫（過渡時 1200ms，運轉時 3000ms）
    val pulseDuration = if (isTransitioning) 1200 else 3000
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = if (isTransitioning) 0.65f else 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoronaBreathPulse"
    )

    // 實體按鍵下壓 Spring 物理彈性縮放（過渡期間鎖定不可按下）
    val puckScale by animateFloatAsState(
        targetValue = if (isPressed && !isTransitioning) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 550f),
        label = "PuckSpringScale"
    )

    // 核心圖示色調過渡：由 ServerState 精準決定
    val emblemColor by animateColorAsState(
        targetValue = when (serverState) {
            is ServerState.Running -> Color(0xFF2563EB)
            is ServerState.Starting -> Color(0xFF38BDF8)
            is ServerState.Stopping -> Color(0xFF60A5FA)
            is ServerState.Failed -> Color(0xFFE11D48)
            is ServerState.Stopped -> Color(0xFF334155)
        },
        animationSpec = tween(durationMillis = 300),
        label = "EmblemColor"
    )

    val showGlow = isRunning || isTransitioning

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // 底層畫布：輕盈通透的高階環境微光
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)

                    val maxGlowRadius = 88.dp.toPx() + (5.dp.toPx() * pulseProgress)

                    val etherealAuraBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF3B82F6).copy(alpha = 0.30f * pulseProgress),
                            0.48f to Color(0xFF3B82F6).copy(alpha = 0.28f * pulseProgress),
                            0.54f to Color(0xFF38BDF8).copy(alpha = 0.26f * pulseProgress),
                            0.66f to Color(0xFF60A5FA).copy(alpha = 0.16f * pulseProgress),
                            0.78f to Color(0xFF93C5FD).copy(alpha = 0.08f * pulseProgress),
                            0.89f to Color(0xFFBAE6FD).copy(alpha = 0.03f * pulseProgress),
                            1.00f to Color(0x00BAE6FD)
                        ),
                        center = center,
                        radius = maxGlowRadius
                    )

                    onDrawBehind {
                        if (showGlow) {
                            drawCircle(
                                brush = etherealAuraBrush,
                                radius = maxGlowRadius,
                                center = center
                            )
                        }
                    }
                }
        )

        // 上層：96dp 純白陶瓷實體圓盤按鈕（過渡中自動禁用點擊防抖）
        Surface(
            modifier = Modifier
                .size(96.dp)
                .scale(puckScale)
                .shadow(
                    elevation = if (isPressed && !isTransitioning) 2.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = Color(0x120F172A),
                    ambientColor = Color(0x060F172A)
                )
                .clip(CircleShape)
                .clickable(
                    enabled = !isTransitioning,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(
                width = 1.2.dp,
                color = when (serverState) {
                    is ServerState.Running -> Color(0xFFBFDBFE)
                    is ServerState.Starting, is ServerState.Stopping -> Color(0xFFBAE6FD)
                    is ServerState.Failed -> Color(0xFFFECDD3)
                    is ServerState.Stopped -> Color(0xFFF1F5F9)
                }
            )
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
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
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isRunning) "停止本地服務" else "啟動本地伺服器",
                    tint = emblemColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
