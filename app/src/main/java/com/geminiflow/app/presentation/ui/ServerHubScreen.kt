package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.geminiflow.app.presentation.components.NavigationBarsSafeSpacer
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
 * 本地伺服器主控台畫面 (無底部導航欄旗艦版)。
 *
 * 架構特點：
 * 1. 導航進化：徹底移除底部導航欄，沙盒與設定以純 Icon 按鈕置於右上角。
 * 2. 延伸介面右滑退棧：沙盒與設定作為延伸介面，進入後右滑手勢直接返回主畫面，無返回按鈕干擾。
 * 3. 視覺焦點明確：動態引擎示波儀居中展開，圖形 100% 無遮蔽。
 */
@Composable
fun ServerHubScreen(
    viewModel: MainViewModel,
    onNavigateToSandbox: () -> Unit,
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
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. 頂部工程品牌標題與快捷導航按鈕 (沙盒 + 設定，純 Icon 按鈕)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 沙盒入口按鈕 (純 Icon，無文字)
                    IconButton(
                        onClick = onNavigateToSandbox,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "模型沙盒",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 設定入口按鈕 (純 Icon，無文字)
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "進階設定",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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

            // 5. 底部自適應導航列安全留白
            NavigationBarsSafeSpacer(extraHeight = 32.dp)
        }
    }
}
