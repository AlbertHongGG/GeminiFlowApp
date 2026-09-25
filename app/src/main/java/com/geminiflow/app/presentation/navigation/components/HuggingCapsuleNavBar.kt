package com.geminiflow.app.presentation.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.geminiflow.app.presentation.navigation.model.AppTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 緊緻包裹正圓毛玻璃導航欄 (HuggingCapsuleNavBar)。
 *
 * 架構亮點：
 * 1. 物理單一滑動實體圓盤（Single Sliding Indicator）：
 *    - 徹底根除條件式 Modifier.shadow 造成的節點突變閃爍與生硬跳躍。
 *    - 整體導航欄內部僅有一枚純白立體浮雕正圓盤（#FFFFFF, elevation = 4dp），
 *      透過微彈性阻尼彈簧物理曲線（Spring Physics）在軌道上平滑滑行。
 * 2. 緊緻自適應包覆結構（Hugging Wrap）：
 *    - 依據按鈕內容自適應緊緻包裹（總寬度約 194dp），徹底杜絕左右兩側空洞留白。
 * 3. Apple VisionOS 晶透白瓷浮島風格：
 *    - 滑動選中圓盤：純白浮雕圓盤搭配深邃石墨黑圖標（#0F172A）。
 *    - 未選中項目：透明底，溫潤石板灰圖標（#64748B）。
 *    - 膠囊背板：70% 高透純白真毛玻璃（Backdrop Blur 24dp）與 1dp 細緻微黑邊框。
 */
@Composable
fun HuggingCapsuleNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    spec: HuggingNavSpec = remember { HuggingNavSpec() }
) {
    val entries = remember { AppTab.entries }
    val selectedIndex = entries.indexOf(selectedTab).coerceAtLeast(0)
    val containerShape = remember(spec.containerCornerRadius) {
        RoundedCornerShape(spec.containerCornerRadius)
    }

    // 計算滑動指示器的目標 X 軸偏移，並透過物理阻尼彈簧實現絲滑軌道平移
    val targetOffset = spec.calculateIndicatorOffset(selectedIndex)
    val animatedIndicatorOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = spec.springDampingRatio,
            stiffness = spec.springStiffness
        ),
        label = "slidingIndicatorOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .shadow(
                    elevation = spec.elevation,
                    shape = containerShape,
                    spotColor = spec.spotShadowColor,
                    ambientColor = spec.ambientShadowColor
                )
                .clip(containerShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(state = hazeState) {
                            blurRadius = spec.blurRadius
                            backgroundColor = spec.glassBackgroundColor
                        }
                    } else {
                        Modifier.background(spec.fallbackBackgroundColor)
                    }
                )
                .border(
                    width = spec.borderWidth,
                    color = spec.borderColor,
                    shape = containerShape
                )
                .padding(
                    horizontal = spec.horizontalPadding,
                    vertical = spec.verticalPadding
                )
        ) {
            // 1. 中層：單一物理實體滑動浮雕圓盤 (Sliding Indicator)
            // 恆定持有 4dp 立體陰影，永不銷毀、永不拔除修飾符，在軌道上平滑滑行
            Box(
                modifier = Modifier
                    .offset(x = animatedIndicatorOffset)
                    .size(spec.itemDiameter)
                    .shadow(
                        elevation = spec.activeCircleElevation,
                        shape = CircleShape,
                        spotColor = spec.activeCircleSpotShadow,
                        ambientColor = spec.activeCircleAmbientShadow
                    )
                    .clip(CircleShape)
                    .background(spec.activeCircleColor)
            )

            // 2. 頂層：按鈕與圖標群 (Row)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spec.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) spec.activeIconColor else spec.inactiveIconColor,
                        animationSpec = tween(spec.iconColorAnimationMillis),
                        label = "iconTint"
                    )

                    Box(
                        modifier = Modifier
                            .size(spec.itemDiameter)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = spec.itemDiameter / 2),
                                onClick = { onTabSelected(tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.spec.icon,
                            contentDescription = tab.spec.contentDescription,
                            modifier = Modifier.size(spec.iconSize),
                            tint = iconTint
                        )
                    }
                }
            }
        }
    }
}
