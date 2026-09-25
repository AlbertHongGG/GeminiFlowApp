package com.geminiflow.app.presentation.ui

import com.geminiflow.app.presentation.viewmodel.MainViewModel
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.GfCopyChip
import com.geminiflow.app.presentation.components.GfStatusBeacon
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.theme.AppColors

/**
 * ServerHubScreen: Clean, un-cluttered main dashboard and server management view
 * Strictly follows LensWise color palette and typography.
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

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. Headerless Hero Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GeminiFlow",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.textPrimaryLight,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LOCAL REVERSE-PROXY ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textSecondaryLight,
                        letterSpacing = 1.5.sp
                    )
                }

                GfStatusBeacon(
                    isOnline = uiState.isServerRunning,
                    label = if (uiState.isServerRunning) "運作中" else "已停止"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Master Server Management Card (LensWise style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.04f),
                        ambientColor = Color.Black.copy(alpha = 0.03f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val serverUrl = "http://${uiState.serverHost}:${uiState.serverPort}"
                    GfCopyChip(
                        text = serverUrl,
                        toastMessage = "已複製伺服器端點網址"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    GfButton(
                        text = if (uiState.isServerRunning) "停止伺服器服務" else "啟動本地伺服器",
                        onClick = { viewModel.toggleServer(context) },
                        variant = if (uiState.isServerRunning) GfButtonVariant.Danger else GfButtonVariant.Primary,
                        leadingIcon = if (uiState.isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                        height = 50.dp,
                        fontSize = 15.sp
                    )

                    if (!uiState.serverErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "錯誤：${uiState.serverErrorMessage}",
                            color = AppColors.danger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Metrics 2x2 Grid (LensWise Card style)
            Text(
                text = "服務度量統計",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondaryLight,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "累計處理請求",
                    value = "${uiState.totalRequests}",
                    subValue = "All Requests",
                    icon = Icons.Default.CloudSync,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "活躍並發連線",
                    value = "${uiState.activeConnections}",
                    subValue = "Active Streams",
                    icon = Icons.Default.Bolt,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "服務運行時長",
                    value = uiState.uptimeFormatted,
                    subValue = if (uiState.isServerRunning) "即時運行中" else "尚未啟動",
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "本機圖床快取",
                    value = uiState.cacheSizeFormatted,
                    subValue = "${uiState.cacheFilesCount} 個圖檔",
                    icon = Icons.Default.Image,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Quick Guardian Checklist (LensWise style)
            Text(
                text = "守護就緒狀態",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondaryLight,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Auth Tile
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLogin() }
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = Color.Black.copy(alpha = 0.04f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = AppColors.surfaceLight
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (uiState.isAuthenticated) AppColors.success else AppColors.warning).copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.isAuthenticated) AppColors.success else AppColors.warning,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google 憑證",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimaryLight
                            )
                            Text(
                                text = if (uiState.isAuthenticated) "已驗證" else "未登入",
                                fontSize = 11.sp,
                                color = if (uiState.isAuthenticated) AppColors.success else AppColors.warning
                            )
                        }
                    }
                }

                // Battery Optimization Tile
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenBatteryGuide() }
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = Color.Black.copy(alpha = 0.04f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = AppColors.surfaceLight
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (uiState.isBatteryUnrestricted) AppColors.success else AppColors.danger).copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isBatteryUnrestricted) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (uiState.isBatteryUnrestricted) AppColors.success else AppColors.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "電池防殺",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimaryLight
                            )
                            Text(
                                text = if (uiState.isBatteryUnrestricted) "無限制" else "待設定",
                                fontSize = 11.sp,
                                color = if (uiState.isBatteryUnrestricted) AppColors.success else AppColors.danger
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(14.dp),
            spotColor = Color.Black.copy(alpha = 0.04f)
        ),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.surfaceLight
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textSecondaryLight
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimaryLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = AppColors.textSecondaryLight
            )
        }
    }
}
