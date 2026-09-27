package com.geminiflow.app.presentation.features.server

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.NavigationBarsSafeSpacer
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 主畫面儀表板（Server Hub Screen）
 * 嚴格保留原版 Header（標題與右上角導航按鈕）。
 * 底層版面全面重構為【一體式極簡懸浮控制中樞】與【動力反應核心主體】。
 */
@Composable
fun ServerHubScreen(
    viewModel: ServerHubViewModel,
    onNavigateToSandbox: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AzureTheme.backgroundSnow)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 原裝 Header 區塊（嚴格 100% 保留，完全不作修改）
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

            Spacer(modifier = Modifier.height(20.dp))

            // 一體化核心主座（以動力反應核心為視覺主角）
            MonolithicEngineChassis(
                isRunning = uiState.isServerRunning,
                host = uiState.serverHost,
                port = uiState.serverPort,
                errorMessage = uiState.serverErrorMessage,
                onToggleServer = { viewModel.toggleServer(context) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 水平流體數據軸卡片（徹底取代 2x2 玩具方塊）
            TelemetryRibbonsCard(
                isRunning = uiState.isServerRunning,
                totalRequests = uiState.totalRequests,
                activeConnections = uiState.activeConnections,
                cacheSizeFormatted = uiState.cacheSizeFormatted,
                cacheFilesCount = uiState.cacheFilesCount
            )

            NavigationBarsSafeSpacer(extraHeight = 32.dp)
        }
    }
}
