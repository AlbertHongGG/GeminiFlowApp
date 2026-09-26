package com.geminiflow.app.presentation.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.AppSwitch
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.NavigationBarsSafeSpacer
import com.geminiflow.app.presentation.components.PremiumConfigHeader
import com.geminiflow.app.presentation.components.SwipeToConfirmDrawer
import com.geminiflow.app.presentation.features.settings.components.SettingsDivider
import com.geminiflow.app.presentation.features.settings.components.SettingsSection
import com.geminiflow.app.presentation.features.settings.components.SettingsTile
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.presentation.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: SettingsViewModel,
    onNavigateToLogin: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onNavigateToAiLogs: () -> Unit,
    onNavigateToNotificationLogs: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showNetworkBottomSheet by remember { mutableStateOf(false) }
    var showClearCacheDrawer by remember { mutableStateOf(false) }
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
                NotificationController.showSuccess("網路設定已儲存（重啟後生效）")
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

    if (showClearCacheDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR IMAGE CACHE",
            onConfirm = {
                viewModel.clearCache()
                NotificationController.showSuccess("已清空所有暫存快取圖片")
                showClearCacheDrawer = false
            },
            onDismiss = { showClearCacheDrawer = false }
        )
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        AppSwitch(
                            checked = uiState.autoStartOnBoot,
                            onCheckedChange = { viewModel.setAutoStartOnBoot(it) }
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
                    icon = Icons.Default.Notifications,
                    iconColor = AppColors.primary,
                    title = "系統通知日誌",
                    onTap = onNavigateToNotificationLogs,
                    trailing = {
                        AppSwitch(
                            checked = uiState.isNotificationLoggingEnabled,
                            onCheckedChange = { viewModel.setNotificationLoggingEnabled(it) }
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.DataObject,
                    iconColor = AppColors.primary,
                    title = "API 請求日誌",
                    onTap = onNavigateToAiLogs,
                    trailing = {
                        AppSwitch(
                            checked = uiState.isApiLoggingEnabled,
                            onCheckedChange = { viewModel.setApiLoggingEnabled(it) }
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Image,
                    iconColor = AppColors.primary,
                    title = "圖床快取",
                    subtitle = "${uiState.cacheSizeFormatted} · ${uiState.cacheFilesCount} 個檔案",
                    trailing = {
                        IconButton(
                            onClick = { showClearCacheDrawer = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "清空快取",
                                tint = AppColors.danger,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                            NotificationController.showWarning("已清除 Google 憑證")
                        }
                    )
                }
            }

            NavigationBarsSafeSpacer(extraHeight = 36.dp)
        }
    }
}
