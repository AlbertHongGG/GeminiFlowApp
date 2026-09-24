package com.geminiflow.app.presentation.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.GfCard
import com.geminiflow.app.presentation.components.GfCopyChip
import com.geminiflow.app.presentation.components.GfMetricTile
import com.geminiflow.app.presentation.components.GfStatusBeacon
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
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary
import com.geminiflow.app.presentation.viewmodel.MainViewModel

/**
 * ServerHubScreen: Headerless Pure Light Mode Master Control Hub
 */
@Composable
fun ServerHubScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // 1. Immersive Hero Header (No TopAppBar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GeminiFlow",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Local Reverse-Proxy Engine",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            GfStatusBeacon(
                isOnline = uiState.isServerRunning,
                label = if (uiState.isServerRunning) "運作中" else "已停止"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Master Power & Endpoint Card
        GfCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Endpoint copy chip
                val serverUrl = "http://${uiState.serverHost}:${uiState.serverPort}"
                GfCopyChip(
                    text = serverUrl,
                    toastMessage = "已複製伺服器端點網址"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Master Power Button (Pure Flat, No Solid Gradient)
                GfButton(
                    text = if (uiState.isServerRunning) "停止伺服器服務" else "啟動本地伺服器",
                    onClick = { viewModel.toggleServer(context) },
                    variant = if (uiState.isServerRunning) GfButtonVariant.Danger else GfButtonVariant.Success,
                    leadingIcon = if (uiState.isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52.dp,
                    fontSize = 16.sp
                )

                if (!uiState.serverErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "錯誤：${uiState.serverErrorMessage}",
                        color = AccentRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. System Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GfMetricTile(
                title = "累計處理請求",
                value = "${uiState.totalRequests}",
                subValue = "All Requests",
                icon = Icons.Default.CloudSync,
                iconTint = AccentBlue,
                modifier = Modifier.weight(1f)
            )
            GfMetricTile(
                title = "活躍並發連線",
                value = "${uiState.activeConnections}",
                subValue = "Active Streams",
                icon = Icons.Default.Bolt,
                iconTint = AccentAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GfMetricTile(
                title = "服務運行時長",
                value = uiState.uptimeFormatted,
                subValue = if (uiState.isServerRunning) "即時運行中" else "伺服器未啟動",
                icon = Icons.Default.HourglassTop,
                iconTint = AccentEmerald,
                modifier = Modifier.weight(1f)
            )
            GfMetricTile(
                title = "本機圖床暫存",
                value = uiState.cacheSizeFormatted,
                subValue = "${uiState.cacheFilesCount} 張圖片",
                icon = Icons.Default.Image,
                iconTint = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. System Readiness & Guardian Checklist Header
        Text(
            text = "系統守護狀態",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Readiness Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Google Auth Tile
            GfCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLogin() },
                contentPadding = 12.dp,
                backgroundColor = if (uiState.isAuthenticated) AccentEmeraldLight else AccentAmberLight,
                borderColor = if (uiState.isAuthenticated) AccentEmerald.copy(alpha = 0.3f) else AccentAmber.copy(alpha = 0.3f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (uiState.isAuthenticated) AccentEmerald else AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google 憑證",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isAuthenticated) AccentEmerald else AccentAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.isAuthenticated) "已驗證" else "未登入 (點擊)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Battery Optimization Tile
            GfCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenBatteryGuide() },
                contentPadding = 12.dp,
                backgroundColor = if (uiState.isBatteryUnrestricted) AccentEmeraldLight else AccentAmberLight,
                borderColor = if (uiState.isBatteryUnrestricted) AccentEmerald.copy(alpha = 0.3f) else AccentAmber.copy(alpha = 0.3f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isBatteryUnrestricted) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (uiState.isBatteryUnrestricted) AccentEmerald else AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "電池防殺",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isBatteryUnrestricted) AccentEmerald else AccentAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.isBatteryUnrestricted) "無限制" else "需設定 (點擊)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Leave space for floating dock
    }
}
