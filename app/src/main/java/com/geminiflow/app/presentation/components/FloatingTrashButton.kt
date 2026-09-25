package com.geminiflow.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
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

/**
 * 懸浮垃圾桶按鈕 (FloatingTrashButton)，復刻 LensWise 的磨砂懸浮圓形垃圾桶設計。
 * 點擊開啟清空抽屜，亦支援懸停狀態動畫。
 */
@Composable
fun FloatingTrashButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHovering: Boolean = false
) {
    val isDark = isSystemInDarkTheme()

    val size by animateDpAsState(
        targetValue = if (isHovering) 64.dp else 56.dp,
        label = "trashSize"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHovering -> Color(0xFFEF4444).copy(alpha = 0.9f)
            isDark -> Color.Black.copy(alpha = 0.65f)
            else -> Color.White.copy(alpha = 0.90f)
        },
        label = "trashBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isHovering -> Color(0xFFEF4444)
            isDark -> Color.White.copy(alpha = 0.12f)
            else -> Color.Black.copy(alpha = 0.10f)
        },
        label = "trashBorder"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isHovering -> Color.White
            isDark -> Color.White.copy(alpha = 0.8f)
            else -> Color.Black.copy(alpha = 0.65f)
        },
        label = "trashIcon"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (isHovering) 16.dp else 10.dp,
                shape = CircleShape,
                ambientColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.15f),
                spotColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isHovering) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isHovering) Icons.Filled.DeleteForever else Icons.Outlined.DeleteOutline,
            contentDescription = "清空日誌",
            tint = iconColor,
            modifier = Modifier.size(if (isHovering) 28.dp else 24.dp)
        )
    }
}
