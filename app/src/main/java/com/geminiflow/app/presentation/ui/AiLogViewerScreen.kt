package com.geminiflow.app.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.data.storage.ApiLogManager
import com.geminiflow.app.data.storage.ApiLogModel
import com.geminiflow.app.presentation.components.ClearLogsDrawer
import com.geminiflow.app.presentation.components.EdgeSwipeBackDetector
import com.geminiflow.app.presentation.components.FloatingTrashButton
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.theme.AppColors
import kotlinx.coroutines.launch

/**
 * API 請求日誌列表頁面，100% 復刻 LensWise AiLogViewerScreen 與使用者截圖二。
 * 包含頂部類別 Tab、機器人卡片列表、箭頭進入詳情、滑動刪除與底部懸浮垃圾桶。
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                                        ambientColor = Color.Black.copy(alpha = 0.04f),
                                        spotColor = Color.Black.copy(alpha = 0.06f)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tabBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = { selectedTabIndex = index }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = tabTextColor
                                )
                            }
                        }
                    }
                }

                // 列表內容區
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
                            ApiLogCardItem(
                                log = log,
                                isDark = isDark,
                                onClick = { onNavigateToDetail(log.rawJson) },
                                onDelete = {
                                    scope.launch {
                                        apiLogManager.clearLog(log.file)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 底部磨砂懸浮垃圾桶
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                FloatingTrashButton(
                    onClick = { showClearDrawer = true }
                )
            }

            // 最左邊緣右滑返回手勢監聽
            EdgeSwipeBackDetector(
                onNavigateBack = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

/**
 * 單筆 API 請求日誌卡片元件，具備機器人圖標、名稱、時間與向右箭頭，支援左右滑動刪除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiLogCardItem(
    log: ApiLogModel,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    AnimatedVisibility(
        visible = true,
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Color(0xFFEF4444).copy(alpha = 0.2f)
                } else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                )
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.02f),
                            spotColor = Color.Black.copy(alpha = 0.04f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onClick
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
                                contentDescription = "AI Agent",
                                tint = AppColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 標題與時間戳
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
        )
    }
}
