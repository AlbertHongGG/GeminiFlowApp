package com.geminiflow.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.AccentEmerald
import com.geminiflow.app.presentation.theme.AccentRose
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.SurfaceElevated
import com.geminiflow.app.presentation.theme.TextPrimary

enum class GfButtonVariant {
    Primary,
    Success,
    Danger,
    Secondary,
    Ghost
}

/**
 * GfButton: Flat, high-end, zero solid gradient button.
 */
@Composable
fun GfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GfButtonVariant = GfButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    fontSize: TextUnit = 15.sp,
    cornerRadius: Dp = 12.dp
) {
    val (bgColor, contentColor, borderStroke) = when (variant) {
        GfButtonVariant.Primary -> Triple(AccentBlue, SurfaceCard, null)
        GfButtonVariant.Success -> Triple(AccentEmerald, SurfaceCard, null)
        GfButtonVariant.Danger -> Triple(AccentRose, SurfaceCard, null)
        GfButtonVariant.Secondary -> Triple(SurfaceElevated, TextPrimary, BorderStroke(1.dp, BorderLight))
        GfButtonVariant.Ghost -> Triple(Color.Transparent, AccentBlue, null)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = contentColor,
            disabledContainerColor = SurfaceElevated,
            disabledContentColor = TextPrimary.copy(alpha = 0.38f)
        ),
        border = borderStroke,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize
            )
        }
    }
}
