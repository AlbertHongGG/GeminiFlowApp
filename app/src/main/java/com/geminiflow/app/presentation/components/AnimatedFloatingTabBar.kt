package com.geminiflow.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 底部導航欄項目資料模型。
 */
data class FloatingTabItem(
    val icon: ImageVector,
    val label: String
)

enum class AppTab(val tabItem: FloatingTabItem) {
    DASHBOARD(FloatingTabItem(Icons.Default.Dns, "主介面")),
    SANDBOX(FloatingTabItem(Icons.Default.Terminal, "沙盒")),
    SETTINGS(FloatingTabItem(Icons.Default.Tune, "設定"))
}

private val EaseOutCubic = CubicBezierEasing(0.215f, 0.610f, 0.355f, 1.0f)

/**
 * 底部膠囊浮動導航欄，支援 300ms 平滑展開與切換動畫。
 */
@Composable
fun AnimatedFloatingTabBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(30.dp),
            color = AppColors.surfaceLight
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (tab in AppTab.entries) {
                    val isSelected = tab == selectedTab
                    FloatingTabBarItem(
                        item = tab.tabItem,
                        isSelected = isSelected,
                        onTap = { onTabSelected(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTabBarItem(
    item: FloatingTabItem,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val duration = 300

    val unselectedColor = Color.Black.copy(alpha = 0.54f)
    val primaryColor = AppColors.primary

    val itemBgColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(durationMillis = duration, easing = EaseOutCubic),
        label = "itemBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else unselectedColor,
        animationSpec = tween(durationMillis = duration, easing = EaseOutCubic),
        label = "contentColor"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 16.dp,
        animationSpec = tween(durationMillis = duration, easing = EaseOutCubic),
        label = "horizontalPadding"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(itemBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTap()
            }
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(duration, easing = EaseOutCubic)) + expandHorizontally(
                    animationSpec = tween(duration, easing = EaseOutCubic),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(tween(150)) + shrinkHorizontally(
                    animationSpec = tween(150),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
