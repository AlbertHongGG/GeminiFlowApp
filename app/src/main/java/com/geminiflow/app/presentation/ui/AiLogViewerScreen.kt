package com.geminiflow.app.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.domain.model.TrafficFilter
import com.geminiflow.app.domain.model.TrafficLog
import com.geminiflow.app.presentation.theme.AppColors
import com.geminiflow.app.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI 解析與網路請求日誌監控畫面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLogViewerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredLogs = uiState.filteredLogs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI 解析與請求日誌",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimaryLight
                        )
                        Text(
                            text = "即時監聽外部呼叫 (${uiState.trafficLogs.size} 筆紀錄)",
                            fontSize = 12.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = AppColors.textPrimaryLight
                        )
                    }
                },
                actions = {
                    if (uiState.trafficLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearTrafficLogs() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "清空日誌",
                                tint = AppColors.textSecondaryLight
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.surfaceLight)
            )
        },
        containerColor = AppColors.backgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrafficFilter.values().forEach { filter ->
                    val isSelected = uiState.trafficFilter == filter
                    Surface(
                        modifier = Modifier.clickable { viewModel.setTrafficFilter(filter) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.12f) else AppColors.surfaceLight,
                        border = BorderStroke(1.dp, if (isSelected) AppColors.primary else Color.Black.copy(alpha = 0.08f)),
                        shadowElevation = if (isSelected) 0.dp else 2.dp
                    ) {
                        Text(
                            text = filter.label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AppColors.primary else AppColors.textSecondaryLight
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = AppColors.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "目前沒有系統日誌",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "第三方 App 發起請求時，紀錄將即時呈現於此",
                            fontSize = 12.sp,
                            color = AppColors.textSecondaryLight
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            TrafficLogCard(log = log)
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 單筆流量日誌卡片元件。
 */
@Composable
private fun TrafficLogCard(log: TrafficLog) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when {
        log.statusCode in 200..299 -> AppColors.success
        log.statusCode in 400..499 -> AppColors.warning
        else -> AppColors.danger
    }

    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        color = AppColors.surfaceLight,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AppColors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = AppColors.primary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${log.method} ${log.path}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimaryLight,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${log.statusCode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeStr • ${log.durationMs}ms • ${log.clientIp}",
                        fontSize = 12.sp,
                        color = AppColors.textSecondaryLight
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.textSecondaryLight
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    if (!log.promptSummary.isNullOrBlank()) {
                        Text(
                            text = "提問摘要：",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textSecondaryLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.promptSummary,
                            fontSize = 12.sp,
                            color = AppColors.textPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!log.responseSummary.isNullOrBlank()) {
                        Text(
                            text = "回應摘要：",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textSecondaryLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.responseSummary,
                            fontSize = 12.sp,
                            color = if (log.isError) AppColors.danger else AppColors.textPrimaryLight
                        )
                    }
                }
            }
        }
    }
}
