package com.geminiflow.app.presentation.features.cacheviewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMetadataDrawer(
    item: CachedImageItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val formattedSize = when {
        item.sizeBytes < 1024 -> "${item.sizeBytes} B"
        item.sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", item.sizeBytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", item.sizeBytes / (1024.0 * 1024.0))
    }

    ImmersiveBottomSheet(
        title = "圖片詳細資訊",
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            MetadataItemRow(
                label = "檔案名稱",
                value = item.filename,
                isMonospace = true
            )

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            MetadataItemRow(
                label = "建立日期",
                value = item.formattedDate
            )

            if (!item.modelName.isNullOrBlank()) {
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                MetadataItemRow(
                    label = "AI 模型",
                    value = item.modelName
                )
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (item.width > 0 && item.height > 0) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetadataCompactCard(
                            label = "解析度",
                            value = "${item.width} × ${item.height}"
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetadataCompactCard(
                        label = "檔案大小",
                        value = formattedSize
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetadataCompactCard(
                        label = "圖片格式",
                        value = item.file.extension.uppercase()
                    )
                }
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            MetadataItemRow(
                label = "儲存路徑",
                value = item.file.absolutePath,
                isMonospace = true,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun MetadataItemRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    maxLines: Int = 1
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.85f),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetadataCompactCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
            )
        }
    }
}
