package com.geminiflow.app.presentation.features.server.aurora

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geminiflow.app.domain.model.server.ServerState
import kotlinx.coroutines.launch

/**
 * 懸浮磨砂玻璃核心按鍵（Tactile Glass Core）
 * 1. 還原 GitChecker Layer 4 的圓形玻璃擬態核心。
 * 2. 具備 Spring 物理反饋與點擊擴散光波（Click Ripple）。
 * 3. 核心圖示色調隨 ServerState 進行純淨無損過渡。
 */
@Composable
fun AuroraGlassCore(
    serverState: ServerState,
    dynamicAuroraColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isTransitioning = serverState.isTransitioning

    // Spring 物理按鍵下壓縮放（過渡期間鎖定點擊與按壓）
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed && !isTransitioning) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 550f),
        label = "GlassButtonSpringScale"
    )

    // 點擊擴散漣漪動畫控制器
    val rippleScale = remember { Animatable(0.8f) }
    val rippleAlpha = remember { Animatable(0f) }

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // 點擊擴散光波層（Click Ripple Layer）
        if (rippleAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(rippleScale.value)
                    .clip(CircleShape)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = dynamicAuroraColor.copy(alpha = rippleAlpha.value),
                        ambientColor = dynamicAuroraColor.copy(alpha = rippleAlpha.value)
                    )
            )
        }

        // 磨砂玻璃主體表面
        Surface(
            modifier = Modifier
                .size(118.dp)
                .scale(buttonScale)
                .shadow(
                    elevation = if (serverState.isRunning) 10.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = dynamicAuroraColor.copy(alpha = if (serverState.isRunning) 0.35f else 0.12f),
                    ambientColor = Color(0x080F172A)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isTransitioning
                ) {
                    coroutineScope.launch {
                        rippleScale.snapTo(0.8f)
                        rippleAlpha.snapTo(0.6f)
                        launch {
                            rippleScale.animateTo(2.0f, tween(600))
                        }
                        launch {
                            rippleAlpha.animateTo(0f, tween(600))
                        }
                    }
                    onToggle()
                },
            shape = CircleShape,
            color = Color(0xF8FFFFFF),
            border = BorderStroke(
                width = 1.2.dp,
                color = dynamicAuroraColor.copy(alpha = if (serverState.isRunning) 0.50f else 0.22f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                // 中心圖示：顏色完全與旋轉極光光暈 100% 同頻共振流動
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Server Power Toggle",
                    tint = dynamicAuroraColor,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }
}
