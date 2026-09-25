package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.theme.AppColors
import com.geminiflow.app.presentation.ui.server.EngineCoreVisualizer
import com.geminiflow.app.presentation.ui.server.TelemetryGraphicInstrument
import com.geminiflow.app.presentation.viewmodel.MainViewModel

/**
 * 本地伺服器主控台與狀態監控畫面 (重構旗艦版)。
 *
 * 架構特點：
 * 1. 視覺焦點確立：以 EngineCoreVisualizer 為絕對核心主體，結合 Canvas 原生向量動態刻度環與即時呼吸回饋。
 * 2. 精密圖形化遙測：以 TelemetryGraphicInstrument 呈現火花波形、LED 計量條與等寬微排版時鐘。
 * 3. 告別冗贅資訊：徹底剔除主畫面「守護就緒」靜態卡片，未授權直接由全域 NotificationController 守衛。
 * 4. 零遮蔽排版：底層保留 96dp 安全邊界，與懸浮膠囊導航欄和諧共存。
 */
@Composable
fun ServerHubScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit = {},
    onOpenBatteryGuide: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. 頂部工程品牌標題
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
                        text = "LOCAL AI GATEWAY ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textSecondaryLight,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 核心主體：動態引擎視覺控制台 (EngineCoreVisualizer)
            EngineCoreVisualizer(
                isRunning = uiState.isServerRunning,
                host = uiState.serverHost,
                port = uiState.serverPort,
                errorMessage = uiState.serverErrorMessage,
                onToggleServer = { viewModel.toggleServer(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 次級區域標題：即時遙測
            Text(
                text = "即時遙測 TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // 4. 圖形化精密遙測儀表組 (TelemetryGraphicInstrument)
            TelemetryGraphicInstrument(
                isRunning = uiState.isServerRunning,
                totalRequests = uiState.totalRequests,
                activeConnections = uiState.activeConnections,
                uptimeFormatted = uiState.uptimeFormatted,
                cacheSizeFormatted = uiState.cacheSizeFormatted,
                cacheFilesCount = uiState.cacheFilesCount
            )

            // 5. 預留底部懸浮膠囊導航欄的安全高度
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
