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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.domain.model.TrafficFilter
import com.geminiflow.app.domain.model.TrafficLog
import com.geminiflow.app.presentation.components.GfCard
import com.geminiflow.app.presentation.theme.AccentAmber
import com.geminiflow.app.presentation.theme.AccentAmberLight
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.AccentBlueLight
import com.geminiflow.app.presentation.theme.AccentEmerald
import com.geminiflow.app.presentation.theme.AccentEmeraldLight
import com.geminiflow.app.presentation.theme.AccentRose
import com.geminiflow.app.presentation.theme.AccentRoseLight
import com.geminiflow.app.presentation.theme.BgCanvas
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.SurfaceElevated
import com.geminiflow.app.presentation.theme.TextMuted
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary
import com.geminiflow.app.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TrafficScreen: Real-time Live Request Stream Inspector
 */
@Composable
fun TrafficScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredLogs = uiState.filteredLogs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // 1. Headerless Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "穿透流量監控",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "監聽第三方 App 請求 (${uiState.trafficLogs.size} 筆紀錄)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (uiState.trafficLogs.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearTrafficLogs() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "清空日誌",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Filter Segmented Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrafficFilter.values().forEach { filter ->
                val isSelected = uiState.trafficFilter == filter
                Surface(
                    modifier = Modifier.clickable { viewModel.setTrafficFilter(filter) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AccentBlueLight else SurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) AccentBlue else BorderLight),
                    shadowElevation = if (isSelected) 0.dp else 1.dp
                ) {
                    Text(
                        text = filter.label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AccentBlue else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Request Stream List
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
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "尚無穿透請求紀錄",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "當其他 App 存取 http://${uiState.serverHost}:${uiState.serverPort} 時，請求將即時顯示於此",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        TrafficLogCard(log = log)
                    }
                    item {
                        Spacer(modifier = Modifier.height(84.dp)) // Clearance for floating dock
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficLogCard(log: TrafficLog) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when {
        log.statusCode in 200..299 -> AccentEmerald
        log.statusCode in 400..499 -> AccentAmber
        else -> AccentRose
    }

    val statusBg = when {
        log.statusCode in 200..299 -> AccentEmeraldLight
        log.statusCode in 400..499 -> AccentAmberLight
        else -> AccentRoseLight
    }

    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    GfCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        contentPadding = 14.dp
    ) {
        // Line 1: Method Badge + Monospace Path + Status Code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Method Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (log.method == "POST") AccentBlueLight else SurfaceElevated,
                    border = BorderStroke(1.dp, if (log.method == "POST") AccentBlue.copy(alpha = 0.3f) else BorderLight)
                ) {
                    Text(
                        text = log.method,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.method == "POST") AccentBlue else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Path
                Text(
                    text = log.path,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            // Status Code Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusBg
            ) {
                Text(
                    text = "${log.statusCode}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Line 2: Duration + Client IP + Time + Expand Chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${log.durationMs} ms",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = log.clientIp,
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
            }
        }

        // Expanded Drawer
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(SurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                if (!log.promptSummary.isNullOrBlank()) {
                    Text(
                        text = "請求提問摘要：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.promptSummary,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!log.responseSummary.isNullOrBlank()) {
                    Text(
                        text = "回應結果摘要：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.responseSummary,
                        fontSize = 12.sp,
                        color = if (log.isError) AccentRose else TextPrimary
                    )
                }
            }
        }
    }
}
