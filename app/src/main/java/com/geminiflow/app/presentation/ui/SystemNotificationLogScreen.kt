package com.geminiflow.app.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.geminiflow.app.data.storage.NotificationLogEntry
import com.geminiflow.app.data.storage.NotificationLogManager
import com.geminiflow.app.domain.model.NotificationType
import com.geminiflow.app.presentation.components.ClearLogsDrawer
import com.geminiflow.app.presentation.components.EdgeSwipeBackDetector
import com.geminiflow.app.presentation.components.FloatingTrashButton
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.PremiumConfigHeader
import kotlinx.coroutines.launch

/**
 * 系統通知日誌檢視畫面（完全對齊 LensWise SystemLogViewerScreen 與使用者截圖一）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemNotificationLogScreen(
    logManager: NotificationLogManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val logs by logManager.logsFlow.collectAsState()

    var showClearDrawer by remember { mutableStateOf(false) }

    if (showClearDrawer) {
        ClearLogsDrawer(
            title = "CLEAR SYSTEM LOGS",
            onConfirm = {
                scope.launch {
                    logManager.clearAllLogs()
                    showClearDrawer = false
                }
            },
            onDismiss = { showClearDrawer = false }
        )
    }

    ImmersiveScaffold(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PremiumConfigHeader(
                    title = "系統通知日誌",
                    subtitle = "SYSTEM NOTIFICATION LOG"
                )

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "目前沒有系統通知日誌。",
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
                            items = logs,
                            key = { it.id }
                        ) { entry ->
                            NotificationLogItemCard(
                                entry = entry,
                                isDark = isDark,
                                onDelete = {
                                    scope.launch {
                                        logManager.clearLog(entry.file)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 底部磨砂懸浮垃圾桶 (Floating Trash Can)
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
 * 單筆系統通知卡片元件，支援左右滑動刪除 (Swipe-to-dismiss)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationLogItemCard(
    entry: NotificationLogEntry,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val (typeColor, typeIcon) = when (entry.type) {
        NotificationType.SUCCESS -> Pair(Color(0xFF10B981), Icons.Outlined.CheckCircleOutline)
        NotificationType.ERROR -> Pair(Color(0xFFEF4444), Icons.Outlined.ErrorOutline)
        NotificationType.WARNING -> Pair(Color(0xFFF59E0B), Icons.Outlined.WarningAmber)
        NotificationType.INFO -> Pair(Color(0xFF3B82F6), Icons.Outlined.Info)
    }

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
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(color)
                )
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.03f),
                            spotColor = Color.Black.copy(alpha = 0.05f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 左側圓形底色圖示
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(typeColor.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = entry.type.name,
                                tint = typeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 右側內容區塊
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.type.name.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = typeColor
                                )

                                Text(
                                    text = entry.timestampStr,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.38f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = entry.message,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp,
                                letterSpacing = 0.3.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        )
    }
}
