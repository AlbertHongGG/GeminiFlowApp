package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.ApiLogModel
import com.geminiflow.app.presentation.components.ClearLogsDrawer
import com.geminiflow.app.presentation.components.DragDropTrashContainer
import com.geminiflow.app.presentation.components.FloatingTrashButton
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.draggableToTrash
import com.geminiflow.app.presentation.components.rememberDragDropTrashState
import com.geminiflow.app.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * API 請求日誌列表頁面，100% 復刻 LensWise AiLogViewerScreen 與使用者截圖二。
 * 包含頂部類別 Tab、機器人卡片列表、長按抓起丟入垃圾桶刪除、箭頭進入詳情與全域邊緣滑動返回。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLogViewerScreen(
    apiLogManager: ApiLogManager,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (rawJson: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val logs by apiLogManager.logsFlow.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearDrawer by remember { mutableStateOf(false) }
    val dragDropState = rememberDragDropTrashState<ApiLogModel>()

    // 取得所有分類標籤（預設「全部」，並保留後續來源擴充彈性）
    val tabs = remember(logs) {
        val customSources = logs.map { it.source }.distinct().filter { it != "全部" }
        listOf("全部") + customSources
    }

    val filteredLogs = remember(logs, selectedTabIndex, tabs) {
        if (selectedTabIndex == 0 || selectedTabIndex >= tabs.size) {
            logs
        } else {
            val selectedSource = tabs[selectedTabIndex]
            logs.filter { it.source == selectedSource }
        }
    }

    if (showClearDrawer) {
        ClearLogsDrawer(
            title = "CLEAR AI LOGS",
            onConfirm = {
                scope.launch {
                    apiLogManager.clearAllLogs()
                    showClearDrawer = false
                }
            },
            onDismiss = { showClearDrawer = false }
        )
    }

    ImmersiveScaffold(modifier = modifier) {
        DragDropTrashContainer(
            state = dragDropState,
            onDropOnTrash = { log ->
                scope.launch {
                    apiLogManager.clearLog(log.file)
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 頂部膠囊型 TabBar（復刻 LensWise TabBar 樣式）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.Black.copy(alpha = 0.04f)
                        )
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, tabTitle ->
                            val isSelected = selectedTabIndex == index
                            val tabBg = when {
                                isSelected && isDark -> Color.White.copy(alpha = 0.15f)
                                isSelected -> Color.White
                                else -> Color.Transparent
                            }
                            val tabTextColor = when {
                                isSelected && isDark -> Color.White
                                isSelected -> AppColors.primary
                                isDark -> Color.White.copy(alpha = 0.54f)
                                else -> Color.Black.copy(alpha = 0.54f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .shadow(
                                        elevation = if (isSelected && !isDark) 4.dp else 0.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        spotColor = Color.Black.copy(alpha = 0.08f)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tabBg)
                                    .clickable { selectedTabIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = tabTextColor
                                )
                            }
                        }
                    }
                }

                // 日誌清單區域
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "目前沒有系統日誌。",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = filteredLogs,
                            key = { it.id }
                        ) { log ->
                            val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == log.id
                            ApiLogCardItem(
                                log = log,
                                isDark = isDark,
                                isDragging = isBeingDragged,
                                onClick = { onNavigateToDetail(log.rawJson) },
                                modifier = Modifier.draggableToTrash(
                                    item = log,
                                    state = dragDropState,
                                    feedback = {
                                        ApiLogCardContent(
                                            log = log,
                                            isDark = isDark
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            }

            // 底部磨砂懸浮垃圾桶，支援長按拖曳丟入刪除
            FloatingTrashButton(
                onClick = { showClearDrawer = true },
                isHovering = dragDropState.isHoveringTrash,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .onGloballyPositioned { coordinates ->
                        dragDropState.trashBoundsInWindow = coordinates.boundsInWindow()
                    }
            )
        }
    }
}

/**
 * 單筆 API 請求日誌卡片元件，長按可抓起拖曳丟入垃圾桶，點擊可進入詳情。
 */
@Composable
private fun ApiLogCardItem(
    log: ApiLogModel,
    isDark: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ApiLogCardContent(
        log = log,
        isDark = isDark,
        onClick = onClick,
        modifier = modifier.alpha(if (isDragging) 0.3f else 1.0f)
    )
}

/**
 * API 日誌卡片外觀內容，100% 復刻 LensWise 卡片樣式。
 */
@Composable
private fun ApiLogCardContent(
    log: ApiLogModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左側圓形底色機器人圖標
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppColors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中間名稱與時間
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.displayTime,
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.45f)
                )
            }

            // 進入詳情箭頭
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "查看詳情",
                tint = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
