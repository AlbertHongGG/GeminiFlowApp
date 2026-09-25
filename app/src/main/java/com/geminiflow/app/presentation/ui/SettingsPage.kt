package com.geminiflow.app.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.ExperimentalMaterial3Api
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.PremiumConfigHeader
import com.geminiflow.app.presentation.components.SettingsDivider
import com.geminiflow.app.presentation.components.SettingsSection
import com.geminiflow.app.presentation.components.SettingsTile
import com.geminiflow.app.presentation.theme.AppColors
import com.geminiflow.app.presentation.viewmodel.MainViewModel

/**
 * 系統進階設定頁面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onNavigateToAiLogs: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showNetworkBottomSheet by remember { mutableStateOf(false) }
    var hostInput by remember { mutableStateOf(uiState.serverHost) }
    var portInput by remember { mutableStateOf(uiState.serverPort.toString()) }

    if (showNetworkBottomSheet) {
        ImmersiveBottomSheet(
            title = "設定服務端點",
            onDismiss = { showNetworkBottomSheet = false },
            onConfirm = {
                val port = portInput.toIntOrNull() ?: 5000
                viewModel.updateServerConfig(hostInput.trim(), port)
                showNetworkBottomSheet = false
                Toast.makeText(context, "網路設定已儲存（重啟後生效）", Toast.LENGTH_SHORT).show()
            }
        ) {
            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it },
                label = { Text("監聽 IP 位址", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                placeholder = { Text("127.0.0.1 或 0.0.0.0") },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimaryLight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = AppColors.primary,
                    unfocusedLabelColor = Color(0xFF475569),
                    focusedTextColor = AppColors.textPrimaryLight,
                    unfocusedTextColor = AppColors.textPrimaryLight
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("通訊埠 (Port)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                placeholder = { Text("5000") },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimaryLight
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = AppColors.primary,
                    unfocusedLabelColor = Color(0xFF475569),
                    focusedTextColor = AppColors.textPrimaryLight,
                    unfocusedTextColor = AppColors.textPrimaryLight
                )
            )
        }
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp) // 預留底部導航欄空間
        ) {
            PremiumConfigHeader(
                title = "進階設定",
                subtitle = "SYSTEM CONFIGURATION"
            )

            SettingsSection(title = "系統") {
                SettingsTile(
                    icon = Icons.Default.Dns,
                    iconColor = AppColors.primary,
                    title = "服務端點",
                    subtitle = "http://${uiState.serverHost}:${uiState.serverPort}",
                    onTap = {
                        hostInput = uiState.serverHost
                        portInput = uiState.serverPort.toString()
                        showNetworkBottomSheet = true
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.PowerSettingsNew,
                    iconColor = AppColors.primary,
                    title = "開機自啟動",
                    trailing = {
                        Switch(
                            checked = uiState.autoStartOnBoot,
                            onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.surfaceLight,
                                checkedTrackColor = AppColors.primary,
                                uncheckedThumbColor = AppColors.surfaceLight,
                                uncheckedTrackColor = AppColors.secondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = if (uiState.isBatteryUnrestricted) AppColors.success else AppColors.warning,
                    title = "電池最佳化",
                    subtitle = if (uiState.isBatteryUnrestricted) "已設置無限制" else "受系統限制",
                    onTap = onOpenBatteryGuide
                )
            }

            SettingsSection(title = "AI 引擎與日誌") {
                SettingsTile(
                    icon = Icons.Default.DataObject,
                    iconColor = AppColors.primary,
                    title = "請求穿透日誌",
                    subtitle = "即時記錄 (${uiState.trafficLogs.size} 筆)",
                    onTap = onNavigateToAiLogs
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Image,
                    iconColor = AppColors.primary,
                    title = "圖床快取",
                    subtitle = "${uiState.cacheSizeFormatted} · ${uiState.cacheFilesCount} 個檔案",
                    trailing = {
                        GfButton(
                            text = "清空快取",
                            onClick = {
                                viewModel.clearCache()
                                Toast.makeText(context, "已清空所有暫存快取圖片", Toast.LENGTH_SHORT).show()
                            },
                            variant = GfButtonVariant.Secondary,
                            height = 34.dp,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            SettingsSection(title = "GOOGLE 帳號憑證") {
                SettingsTile(
                    icon = Icons.Default.AccountCircle,
                    iconColor = if (uiState.isAuthenticated) AppColors.success else AppColors.danger,
                    title = "Google 帳號",
                    subtitle = if (uiState.isAuthenticated) "已驗證授權" else "未登入",
                    onTap = onNavigateToLogin
                )
                if (uiState.isAuthenticated) {
                    SettingsDivider()
                    SettingsTile(
                        icon = Icons.Default.DeleteForever,
                        title = "清除登入憑證",
                        subtitle = "註銷目前登入狀態",
                        isDestructive = true,
                        onTap = {
                            viewModel.clearAuth()
                            Toast.makeText(context, "已清除 Google 憑證", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
