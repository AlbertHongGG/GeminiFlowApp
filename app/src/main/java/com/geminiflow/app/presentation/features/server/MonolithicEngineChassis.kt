package com.geminiflow.app.presentation.features.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.notification.NotificationController
import kotlinx.coroutines.delay

/**
 * 一體化核心主座（Monolithic Engine Chassis）
 * 居中承載【動力反應核心主體】為視覺焦點，整合運行狀態與純圖示端點複製卡匣。
 */
@Composable
fun MonolithicEngineChassis(
    isRunning: Boolean,
    host: String,
    port: Int,
    errorMessage: String?,
    onToggleServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val endpointUrl = "http://$host:$port"
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1800)
            isCopied = false
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x0C0F172A),
                ambientColor = Color(0x040F172A)
            ),
        shape = RoundedCornerShape(24.dp),
        color = AzureTheme.cardSurface,
        border = BorderStroke(
            width = 1.2.dp,
            brush = if (isRunning) AzureTheme.activeGlowBrush else AzureTheme.subtleBorderBrush
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 畫面主角：動力反應核心（Kinetic Reactor Core）
            KineticReactorCore(
                isRunning = isRunning,
                onToggle = onToggleServer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 狀態大文字（無冗贅副標，純淨有力）
            Text(
                text = if (isRunning) "ENGINE OPERATIONAL" else "ENGINE DORMANT",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isRunning) AzureTheme.azureDeep else AzureTheme.textDim,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 內嵌式端點卡匣（純圖示按鈕，無多餘文字）
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AzureTheme.slotBackgroundSubtle,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCopied) AzureTheme.azureBorder else AzureTheme.borderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(endpointUrl))
                        isCopied = true
                        NotificationController.showSuccess("已複製服務端點網址")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) AzureTheme.azurePrimary else AzureTheme.textDim)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = endpointUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzureTheme.textHeading,
                            letterSpacing = 0.3.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(endpointUrl))
                            isCopied = true
                            NotificationController.showSuccess("已複製服務端點網址")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        AnimatedVisibility(
                            visible = isCopied,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已複製",
                                tint = AzureTheme.azurePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = !isCopied,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "複製端點",
                                tint = AzureTheme.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 系統異常提示條
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AzureTheme.accentRoseSoft,
                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "系統異常：$errorMessage",
                        color = Color(0xFFBE123C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
