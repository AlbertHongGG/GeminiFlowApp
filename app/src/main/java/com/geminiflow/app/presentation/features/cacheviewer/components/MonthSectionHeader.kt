package com.geminiflow.app.presentation.features.cacheviewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.domain.model.cache.CachedImageMonthGroup
import com.geminiflow.app.presentation.theme.AppColors
import java.util.Locale

@Composable
fun MonthSectionHeader(
    group: CachedImageMonthGroup,
    modifier: Modifier = Modifier
) {
    val formattedSize = when {
        group.totalSizeBytes < 1024 -> "${group.totalSizeBytes} B"
        group.totalSizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", group.totalSizeBytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", group.totalSizeBytes / (1024.0 * 1024.0))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.yearMonthDisplay,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimaryLight,
            letterSpacing = (-0.3).sp
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.05f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${group.itemCount} 張 · $formattedSize",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.55f)
            )
        }
    }
}
