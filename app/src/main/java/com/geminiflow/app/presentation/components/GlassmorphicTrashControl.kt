package com.geminiflow.app.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassmorphicTrashControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isHovering: Boolean = false,
    bottomSafetyPadding: Dp = 28.dp
) {
    val isDark = isSystemInDarkTheme()

    val size by animateDpAsState(
        targetValue = if (isHovering) 64.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "trashSize"
    )

    val borderBrush = if (isHovering) {
        Brush.verticalGradient(
            listOf(Color(0xFFFCA5A5), Color(0xFFB91C1C))
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.50f else 0.90f),
                Color.White.copy(alpha = if (isDark) 0.15f else 0.30f)
            )
        )
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = bottomSafetyPadding)
            .size(size)
            .shadow(
                elevation = if (isHovering) 20.dp else 12.dp,
                shape = CircleShape,
                ambientColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.16f)
            )
            .clip(CircleShape)
            .then(
                if (isHovering) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                        )
                    )
                } else if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = 24.dp
                        noiseFactor = 0.10f
                        backgroundColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.55f)
                    }
                } else {
                    Modifier.background(
                        if (isDark) Color(0xFF0F172A).copy(alpha = 0.65f)
                        else Color.White.copy(alpha = 0.70f)
                    )
                }
            )
            .border(
                width = if (isHovering) 1.5.dp else 1.dp,
                brush = borderBrush,
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
            tint = if (isHovering) Color.White else if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF1E293B).copy(alpha = 0.85f),
            modifier = Modifier.size(if (isHovering) 28.dp else 24.dp)
        )
    }
}
