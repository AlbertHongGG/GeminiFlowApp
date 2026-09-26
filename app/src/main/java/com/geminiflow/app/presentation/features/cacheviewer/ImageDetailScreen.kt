package com.geminiflow.app.presentation.features.cacheviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.geminiflow.app.domain.model.cache.CachedImageItem
import com.geminiflow.app.presentation.features.cacheviewer.components.ImageMetadataDrawer
import com.geminiflow.app.presentation.features.cacheviewer.components.rememberZoomableImageState
import com.geminiflow.app.presentation.theme.AppColors

@Composable
fun ImageDetailScreen(
    item: CachedImageItem?,
    isExporting: Boolean,
    onNavigateBack: () -> Unit,
    onDownload: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val context = LocalContext.current
    val zoomState = rememberZoomableImageState()
    var showInfoDrawer by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(item.filename) {
        zoomState.reset()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 互動式高解析度影像縮放與真幾何平移畫布
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    zoomState.updateDimensions(size, item.width, item.height)
                }
                .pointerInput(item.filename) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            zoomState.handleDoubleTap(tapOffset)
                        }
                    )
                }
                .pointerInput(item.filename) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        zoomState.onTransform(centroid, pan, zoom)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.file)
                    .size(coil.size.Size.ORIGINAL)
                    .precision(coil.size.Precision.EXACT)
                    .crossfade(true)
                    .build(),
                contentDescription = item.filename,
                contentScale = ContentScale.Fit,
                onSuccess = { success ->
                    val drawable = success.result.drawable
                    if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                        zoomState.updateDimensions(
                            zoomState.viewportSize,
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomState.scale.value
                        scaleY = zoomState.scale.value
                        translationX = zoomState.offset.value.x
                        translationY = zoomState.offset.value.y
                    }
            )
        }

        // 頂部操作列：左側無關閉按鈕（由右滑上一頁驅動），右側集中擺放 Info、Download、Delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(1.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info 按鈕：展開下方詳細資訊抽屜面板
                IconButton(
                    onClick = { showInfoDrawer = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "詳細資訊",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 下載/匯出按鈕
                IconButton(
                    onClick = onDownload,
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "儲存至相簿",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 刪除按鈕
                IconButton(
                    onClick = { showDeleteConfirmDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "刪除快取",
                        tint = AppColors.danger,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 下拉式詳細資訊抽屜面板（平常隱藏）
        if (showInfoDrawer) {
            ImageMetadataDrawer(
                item = item,
                onDismiss = { showInfoDrawer = false }
            )
        }

        // 刪除確認對話框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "確定刪除快取圖片？",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "圖片「${item.filename}」將從本機內部儲存空間永久刪除，無法復原。",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteConfirmed()
                        }
                    ) {
                        Text(
                            text = "確認刪除",
                            color = AppColors.danger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text(text = "取消")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
