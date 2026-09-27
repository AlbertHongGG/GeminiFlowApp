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

    // 日冕光暈平緩深層呼吸動畫（3.0 秒極其柔和雙向起伏）
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoronaBreathPulse"
    )

    // 實體按鍵下壓 Spring 物理彈性縮放（按壓時下沉至 0.94x 後彈性復位）
    val puckScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 550f),
        label = "PuckSpringScale"
    )

    // 核心圖示色調過渡
    val emblemColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF2563EB) else Color(0xFF334155),
        animationSpec = tween(durationMillis = 350),
        label = "EmblemColor"
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // 底層畫布：輕盈通透的高階環境微光（剔除深色實體層，以極低不透明度打造真實光學光暈）
        Box(
            modifier = Modifier
                .size(180.dp)
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)

                    // 擴散至外圍的光暈最大半徑（約 88dp ~ 93dp，直徑達 180dp）
                    val maxGlowRadius = 88.dp.toPx() + (5.dp.toPx() * pulseProgress)

                    // 超柔和透光漸層：峰值透明度嚴格控制在 0.30 左右，選用清透亮天藍與湛藍，杜絕濃重深藍與實體感
                    val etherealAuraBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF3B82F6).copy(alpha = 0.30f * pulseProgress),
                            0.48f to Color(0xFF3B82F6).copy(alpha = 0.28f * pulseProgress),
                            0.54f to Color(0xFF38BDF8).copy(alpha = 0.26f * pulseProgress), // 緊貼 48dp 圓盤外側的明亮漫射光
                            0.66f to Color(0xFF60A5FA).copy(alpha = 0.16f * pulseProgress),
                            0.78f to Color(0xFF93C5FD).copy(alpha = 0.08f * pulseProgress),
                            0.89f to Color(0xFFBAE6FD).copy(alpha = 0.03f * pulseProgress),
                            1.00f to Color(0x00BAE6FD) // 終端完全融入純白底色
                        ),
                        center = center,
                        radius = maxGlowRadius
                    )

                    onDrawBehind {
                        if (isRunning) {
                            drawCircle(
                                brush = etherealAuraBrush,
                                radius = maxGlowRadius,
                                center = center
                            )
                        }
                    }
                }
        )

        // 上層：96dp 純白陶瓷實體圓盤按鈕（柔和自然微陰影，不產生重色陰影圈）
        Surface(
            modifier = Modifier
                .size(96.dp)
                .scale(puckScale)
                .shadow(
                    elevation = if (isPressed) 2.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = Color(0x120F172A),
                    ambientColor = Color(0x060F172A)
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
                color = if (isRunning) Color(0xFFE2E8F0) else Color(0xFFF1F5F9)
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
                // 核心電源微標（36dp 高清晰無襯線圖示，置於純白瓷面中央）
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
