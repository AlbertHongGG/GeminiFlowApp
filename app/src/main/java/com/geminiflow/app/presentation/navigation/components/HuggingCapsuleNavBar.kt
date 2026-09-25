package com.geminiflow.app.presentation.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geminiflow.app.presentation.navigation.model.AppTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 緊緻包裹正圓毛玻璃導航欄 (HuggingCapsuleNavBar)。
 *
 * 設計亮點：
 * 1. 緊緻包覆結構 (Hugging Wrap)：膠囊外殼依據按鈕內容自適應緊密包裹，消除多餘左右兩側過寬留白，整體比例協調。
 * 2. 嚴格正圓形指示塊 (CircleShape)：直徑 50dp，純圓形無壓迫感，圖標大氣清晰 (24dp)。
 * 3. Apple VisionOS 晶透白瓷浮島風格：
 *    - 選中項目：純白浮雕圓盤 (#FFFFFF, 4dp 柔和陰影) 搭配深邃石墨黑圖標 (#0F172A)。
 *    - 未選中項目：全透明底，溫潤石板灰圖標 (#64748B)。
 *    - 膠囊背板：70% 高透純白毛玻璃 (Backdrop Blur 24dp) 與 1dp 極細淡墨描邊。
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
    val containerShape = remember(spec.containerCornerRadius) {
        RoundedCornerShape(spec.containerCornerRadius)
    }

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(spec.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val circleColor by animateColorAsState(
                        targetValue = if (isSelected) spec.activeCircleColor else spec.inactiveCircleColor,
                        animationSpec = tween(spec.animationDurationMillis),
                        label = "circleColor"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) spec.activeIconColor else spec.inactiveIconColor,
                        animationSpec = tween(spec.animationDurationMillis),
                        label = "iconTint"
                    )

                    Box(
                        modifier = Modifier
                            .size(spec.itemDiameter)
                            .then(
                                if (isSelected) {
                                    Modifier.shadow(
                                        elevation = spec.activeCircleElevation,
                                        shape = CircleShape,
                                        spotColor = spec.activeCircleSpotShadow,
                                        ambientColor = spec.activeCircleAmbientShadow
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clip(CircleShape)
                            .background(circleColor)
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
