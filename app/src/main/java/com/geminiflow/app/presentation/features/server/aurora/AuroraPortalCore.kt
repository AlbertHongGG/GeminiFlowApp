package com.geminiflow.app.presentation.features.server.aurora

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geminiflow.app.domain.model.server.ServerState

/**
 * 極光星雲門戶核心主體（Aurora Portal Core）
 * 1. 深度還原 GitChecker 的五層互動式極光門戶系統。
 * 2. 結合雙層反向純圓星雲（AuroraNebulaLayer）、雙旋轉軌道與衛星粒子（AuroraOrbitLayer）以及懸浮磨砂玻璃核心（AuroraGlassCore）。
 * 3. 徹底消滅方形邊界，中心開關圖示顏色與旋轉極光光暈 100% 同頻同步變化。
 */
@Composable
fun AuroraPortalCore(
    serverState: ServerState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 依據伺服器狀態解析專屬視覺輪廓
    val profile = remember(serverState) {
        AuroraVisualSpecDefaults.resolveProfile(serverState)
    }

    // 當狀態為 Failed 時使用警示光譜，其餘使用標準 GitChecker 極光光譜
    val nebulaSpec = remember(serverState) {
        if (serverState is ServerState.Failed) {
            AuroraVisualSpecDefaults.AlertNebulaSpec.copy(
                primarySize = 250.dp,
                secondarySize = 200.dp
            )
        } else {
            AuroraVisualSpecDefaults.StandardNebulaSpec.copy(
                primarySize = 250.dp,
                secondarySize = 200.dp
            )
        }
    }

    val orbitSpec = remember {
        AuroraVisualSpecDefaults.StandardOrbitSpec.copy(
            outerTrackDiameter = 195.dp,
            innerTrackDiameter = 150.dp
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AuroraPortalContinuousTransition")

    // 順時針連續旋轉週期：由 profile.speedMultiplier 動態調節速度
    val primaryDuration = (nebulaSpec.primaryDurationMs / profile.speedMultiplier).toInt().coerceAtLeast(1000)
    val secondaryDuration = (nebulaSpec.secondaryDurationMs / profile.speedMultiplier).toInt().coerceAtLeast(1000)

    val primaryRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = primaryDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PortalPrimaryRotation"
    )

    val secondaryRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = secondaryDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PortalSecondaryRotation"
    )

    // 計算即時流動極光光譜色：讓中心按鈕圖示與極光光暈 100% 絕對同頻共振
    val dynamicAuroraColor = remember(primaryRotation, nebulaSpec.primaryColors) {
        AuroraVisualSpecDefaults.evaluateAuroraColor(primaryRotation, nebulaSpec.primaryColors)
    }

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1 & 2: 雙層反向純圓錐形漸層星雲（Skia GPU 著色器，徹底杜絕方形邊界）
        AuroraNebulaLayer(
            spec = nebulaSpec,
            profile = profile,
            primaryRotation = primaryRotation,
            secondaryRotation = secondaryRotation
        )

        // Layer 3 & 3.5: 雙軌道與青/粉發光微星粒子
        AuroraOrbitLayer(
            spec = orbitSpec,
            profile = profile
        )

        // Layer 4: 懸浮磨砂玻璃核心本體（中心圖示與邊框即時流動極光色）
        AuroraGlassCore(
            serverState = serverState,
            dynamicAuroraColor = dynamicAuroraColor,
            onToggle = onToggle
        )
    }
}
