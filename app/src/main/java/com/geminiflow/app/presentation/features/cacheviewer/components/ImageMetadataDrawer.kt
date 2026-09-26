package com.geminiflow.app.presentation.features.cacheviewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.presentation.theme.AppColors
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

            // 檔案名稱：專屬全寬區塊，自動折行不截斷並支援複製
            MetadataBlock(
                label = "檔案名稱",
                value = item.filename,
                isMonospace = true,
                copyLabel = "檔案名稱"
            )

            // 建立日期
            MetadataItemRow(
                label = "建立日期",
                value = item.formattedDate
            )

            // AI 模型（若有則顯示）
            if (!item.modelName.isNullOrBlank()) {
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                MetadataItemRow(
                    label = "AI 模型",
                    value = item.modelName
                )
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))

            // 統計卡片三欄位：解析度、大小、格式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

            // 儲存路徑：專屬全寬區塊，完整呈現路徑並支援一鍵複製
            MetadataBlock(
                label = "本機儲存路徑",
                value = item.file.absolutePath,
                isMonospace = true,
                copyLabel = "儲存路徑"
            )
        }
    }
}

@Composable
private fun MetadataBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    copyLabel: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.45f)
            )
            if (copyLabel != null) {
                Text(
                    text = "複製",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.primary,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(value))
                        NotificationController.showSuccess("已複製 $copyLabel")
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SelectionContainer {
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = if (isDark) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.88f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun MetadataItemRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false
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
            maxLines = 1,
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
            .padding(vertical = 10.dp, horizontal = 6.dp),
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
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
            )
        }
    }
}
