package com.geminiflow.app.presentation.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.GfCard
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.AccentBlueLight
import com.geminiflow.app.presentation.theme.AccentEmerald
import com.geminiflow.app.presentation.theme.AccentRose
import com.geminiflow.app.presentation.theme.BgCanvas
import com.geminiflow.app.presentation.theme.BorderFocused
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.SurfaceElevated
import com.geminiflow.app.presentation.theme.TextMuted
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary
import com.geminiflow.app.presentation.viewmodel.MainViewModel

/**
 * SettingsScreen: Clean, professional light-mode settings viewport
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onOpenBatteryGuide: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hostInput by remember { mutableStateOf(uiState.serverHost) }
    var portInput by remember { mutableStateOf(uiState.serverPort.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // 1. Headerless Title
        Column {
            Text(
                text = "系統設定",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "配置伺服器網路、後台保活與身分憑證",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Server Network Configuration
        GfCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "本機伺服器網路綁定",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it },
                label = { Text("監聽 IP 位址") },
                placeholder = { Text("127.0.0.1 (僅限本機) 或 0.0.0.0 (局域網)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("通訊埠 (Port)") },
                placeholder = { Text("5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            GfButton(
                text = "儲存網路設定",
                onClick = {
                    val port = portInput.toIntOrNull() ?: 5000
                    viewModel.updateServerConfig(hostInput.trim(), port)
                    Toast.makeText(context, "設定已儲存（重啟伺服器後生效）", Toast.LENGTH_SHORT).show()
                },
                leadingIcon = Icons.Default.Save,
                variant = GfButtonVariant.Primary,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Auto-start on Boot
        GfCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "開機自動啟動服務",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "手機重新開機後自動於背景拉起本地伺服器",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = uiState.autoStartOnBoot,
                    onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceCard,
                        checkedTrackColor = AccentBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Battery Optimization & Power Guardian
        GfCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "背景防殺與保活設置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "目前狀態：" + if (uiState.isBatteryUnrestricted) "已放行 (無限制)" else "受限 (建議設置以防後台被休眠殺死)",
                fontSize = 12.sp,
                color = if (uiState.isBatteryUnrestricted) AccentEmerald else AccentRose,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GfButton(
                    text = "防殺設定指南",
                    onClick = onOpenBatteryGuide,
                    variant = GfButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                GfButton(
                    text = "應用程式資訊",
                    onClick = { viewModel.openAppDetailsSettings(context) },
                    variant = GfButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Google Account & Session Management
        GfCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Google 帳號憑證管理",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "憑證狀態：" + if (uiState.isAuthenticated) "已驗證授權 (__Secure-1PSID 有效)" else "未登入 (無法存取 Gemini)",
                fontSize = 12.sp,
                color = if (uiState.isAuthenticated) AccentEmerald else AccentRose,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GfButton(
                    text = if (uiState.isAuthenticated) "重新驗證登入" else "登入 Google 帳號",
                    onClick = onNavigateToLogin,
                    variant = GfButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.isAuthenticated) {
                    GfButton(
                        text = "清除憑證",
                        onClick = {
                            viewModel.clearAuth()
                            Toast.makeText(context, "已清除 Google 認證憑證", Toast.LENGTH_SHORT).show()
                        },
                        variant = GfButtonVariant.Danger,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Image Cache Management
        GfCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "本機生成圖床快取",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "目前佔用容量：${uiState.cacheSizeFormatted} (${uiState.cacheFilesCount} 個圖檔)",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            GfButton(
                text = "立即清空所有快取圖檔",
                onClick = {
                    viewModel.clearCache()
                    Toast.makeText(context, "已清空所有快取圖片", Toast.LENGTH_SHORT).show()
                },
                variant = GfButtonVariant.Secondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. About App Card
        GfCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceElevated
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "GeminiFlow Engine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "版本：1.0.0 (Clean Architecture & Light Mode)\n執行時核心：Ktor CIO Embedded Server",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(88.dp)) // Clearance for floating dock
    }
}
