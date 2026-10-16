package com.geminiflow.app.presentation.components.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import com.geminiflow.app.presentation.components.IsolatedHandleBottomSheet
import com.geminiflow.app.presentation.features.gallery.GalleryPickerViewModel
import com.geminiflow.app.presentation.theme.AppColors

@Composable
fun GalleryPickerBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (List<GalleryMediaItem>) -> Unit,
    maxSelection: Int = 4,
    viewModel: GalleryPickerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 檢查並判定權限狀態
    val permissionsToRequest = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.onPermissionResult(alreadyGranted)
        if (!alreadyGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    IsolatedHandleBottomSheet(
        onDismiss = onDismiss,
        heightRatio = 0.88f,
        header = {
            // 頂部 Handle 與導航列 (此區域是唯一感應向下拖曳以關閉的區域)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 拖曳指示條 (Handle)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(5.dp)
                        .background(Color(0xFFD1D5DB), RoundedCornerShape(2.5.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header 標題與確認按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側佔位 48dp 確保標題絕對置中
                    Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = "選擇照片",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF64748B)
                    )

                    val canConfirm = uiState.selectedItems.isNotEmpty()
                    IconButton(
                        onClick = {
                            if (canConfirm) {
                                onConfirm(uiState.selectedItems)
                                onDismiss()
                            }
                        },
                        enabled = canConfirm,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "確認選擇",
                            tint = if (canConfirm) AppColors.primary else Color(0xFFCBD5E1),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) {
        // 內容展示區 (手勢完全隔離，在照片網格滑動時完全不會連動關閉彈窗)
        if (!uiState.hasPermission && uiState.permissionRequested) {
            PermissionDeniedView(
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onRequestAgain = {
                    permissionLauncher.launch(permissionsToRequest)
                }
            )
        } else if (uiState.allAssets.isEmpty() && uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (uiState.allAssets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "相簿內查無照片",
                    color = AppColors.textSecondaryLight,
                    fontSize = 14.sp
                )
            }
        } else {
            GalleryGridView(
                uiState = uiState,
                maxSelection = maxSelection,
                onToggleSelect = { item -> viewModel.toggleSelection(item, maxSelection) },
                onLoadMore = { viewModel.loadMore() }
            )
        }
    }
}

@Composable
private fun GalleryGridView(
    uiState: com.geminiflow.app.presentation.features.gallery.GalleryPickerUiState,
    maxSelection: Int,
    onToggleSelect: (GalleryMediaItem) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // 監聽滾動至末端載入更多：提早 24 項（約 8 列、2 個螢幕高度）於背景靜默預載，達成無縫順滑捲動
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItemsCount = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 24
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        uiState.groupKeys.forEach { groupKey ->
            val itemsInGroup = uiState.groupedAssets[groupKey].orEmpty()

            // 月份標題橫跨 3 欄
            item(
                key = "header_$groupKey",
                span = { GridItemSpan(3) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = groupKey,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // 該月份下的照片：使用 O(1) selectedIds 與穩定的 onToggleSelect 回調
            items(
                items = itemsInGroup,
                key = { it.id }
            ) { item ->
                val isSelected = uiState.selectedIds.contains(item.id)
                GalleryPhotoThumbnail(
                    item = item,
                    isSelected = isSelected,
                    onToggle = onToggleSelect
                )
            }
        }

        if (uiState.isLoadingMore && uiState.allAssets.isNotEmpty()) {
            item(
                key = "loading_footer",
                span = { GridItemSpan(3) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppColors.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoThumbnail(
    item: GalleryMediaItem,
    isSelected: Boolean,
    onToggle: (GalleryMediaItem) -> Unit
) {
    val context = LocalContext.current

    // 使用 remember 避免每次重組重新建構 ImageRequest 物件
    // 明確設定 360x360 尺寸並關閉 crossfade 動畫，徹底杜絕滑動時 GPU Alpha 動畫堵塞與重複解碼
    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(360, 360)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onToggle(item) }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 選中狀態遮罩、邊框與打勾標記
        if (isSelected) {
            // 半透明暗色遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "已選中",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 主題色外框
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(3.dp, AppColors.primary))
            )
        }
    }
}

@Composable
private fun PermissionDeniedView(
    onOpenSettings: () -> Unit,
    onRequestAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color(0xFFF1F5F9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "需要相簿存取權限",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimaryLight
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "為了讓您在模型沙盒中傳送圖片進行多模態連續測試，請允許存取裝置上的照片。",
            fontSize = 13.sp,
            color = AppColors.textSecondaryLight,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRequestAgain,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
            ) {
                Text("重新授權")
            }
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0))
            ) {
                Text("前往設定", color = Color(0xFF1E293B))
            }
        }
    }
}
