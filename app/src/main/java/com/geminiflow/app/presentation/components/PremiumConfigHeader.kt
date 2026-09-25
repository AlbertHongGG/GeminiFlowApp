package com.geminiflow.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * PremiumConfigHeader: 100% matched to LensWise (lib/core/widgets/premium_config_header.dart)
 * 34sp ExtraBold title, 12sp Bold uppercase letter-spaced subtitle, and 3D ThinkingOrb in background.
 * Provides spacious 38dp top and 24dp bottom breathing space matching LensWise.
 */
@Composable
fun PremiumConfigHeader(
    title: String = "進階設定",
    subtitle: String = "SYSTEM CONFIGURATION",
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 38.dp, bottom = 24.dp)
    ) {
        // Decorative 150dp ThinkingOrb vertically centered on the right
        // Reports (0, 0) layout size so parent Box height is driven by text Row + paddings
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, minHeight = 0)
                    )
                    layout(0, 0) {
                        placeable.placeRelative(
                            -placeable.width + 16.dp.roundToPx(),
                            -placeable.height / 2
                        )
                    }
                }
                .alpha(0.20f)
        ) {
            ThinkingOrb(size = 150.dp, isDark = false)
        }

        // Header Text & Trailing Content
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.textPrimaryLight,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textSecondaryLight,
                    letterSpacing = 1.5.sp
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailing()
            }
        }
    }
}
