package com.geminiflow.app.presentation.features.cacheviewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. 檔案名稱 Hero 識別區：精緻圖示 + 完整檔名 + 純 Icon 複製按鈕
            FileHeroSection(filename = item.filename)

            // 2. 建立日期與 AI 模型：一體化分組卡片
            GroupedMetadataCard(
                date = item.formattedDate,
                modelName = item.modelName
            )

            // 3. 媒體規格三聯卡片：解析度、大小、格式
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
                        value = if (item.format != com.geminiflow.app.domain.model.cache.ImageFormat.UNKNOWN) {
                            item.format.displayName
                        } else {
                            item.file.extension.uppercase()
                        }
                    )
                }
            }

            // 4. 儲存路徑：固定高度、支援拖曳滾動檢視與純 Icon 複製按鈕
            ScrollablePathConsoleBox(path = item.file.absolutePath)
        }
    }
}

/**
 * 檔案名稱 Hero 呈現區塊。
 * 將檔案名稱作為主識別，搭配檔案圖示與純 Icon 複製按鈕，杜絕生硬粗糙外框。
 */
@Composable
private fun FileHeroSection(
    filename: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 檔案圖示小徽章
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 檔名內容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "檔案名稱",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            SelectionContainer {
                Text(
                    text = filename,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color.Black.copy(alpha = 0.88f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 純 Icon 複製按鈕
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(filename))
                NotificationController.showSuccess("已複製檔案名稱")
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "複製檔案名稱",
                tint = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 建立日期與 AI 模型的一體化分組卡片。
 */
@Composable
private fun GroupedMetadataCard(
    date: String,
    modelName: String?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.035f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetadataRow(label = "建立日期", value = date)
        if (!modelName.isNullOrBlank()) {
            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))
            MetadataRow(label = "AI 模型", value = modelName)
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
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
            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 解析度、檔案大小、圖片格式三聯小卡片。
 */
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

/**
 * 儲存路徑固定高度、支援拖曳滾動與純 Icon 複製之代碼控制台區塊。
 */
@Composable
private fun ScrollablePathConsoleBox(
    path: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.55f) else Color(0xFFF8FAFC))
            .border(
                width = 0.8.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 10.dp)
    ) {
        // 頂部小標籤與純 Icon 複製按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "本機儲存路徑",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f)
                )
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(path))
                    NotificationController.showSuccess("已複製儲存路徑")
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "複製儲存路徑",
                    tint = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 固定高度可點擊拖拉垂直滾動的文字視窗
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .verticalScroll(scrollState)
                .padding(end = 6.dp)
        ) {
            SelectionContainer {
                Text(
                    text = path,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
                    lineHeight = 17.sp
                )
            }
        }
    }
}
