package com.geminiflow.app.presentation.features.server

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay

/**
 * 一體化核心主座（Monolithic Engine Chassis）
 * 1. 外框維持原先細緻邊框色彩（非藍色）。
 * 2. 居中承載 98dp 瓷質大按鍵與 184dp 柔焦日冕。
 * 3. ENGINE 狀態與計時作為主標題（● ENGINE ACTIVE · 00:14:32）。
 * 4. Server URL 作為副標題置於 ENGINE 字串下方，文字絕對水平置中，無輸入框包覆、無圓點，右側放置複製 icon 按鈕。
 */
@Composable
fun MonolithicEngineChassis(
    isRunning: Boolean,
    host: String,
    port: Int,
    uptimeFormatted: String = "",
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
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x0C0F172A),
                ambientColor = Color(0x040F172A)
            ),
        shape = RoundedCornerShape(24.dp),
        color = AzureTheme.cardSurface,
        border = BorderStroke(
            width = 1.2.dp,
            brush = AzureTheme.subtleBorderBrush
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 畫面主角：日冕一體化軌道鐘開關主體
            KineticReactorCore(
                isRunning = isRunning,
                onToggle = onToggleServer
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 主標題：單行透氣狀態與計時展示（● ENGINE ACTIVE · 00:14:32）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 發光狀態指示微圓點
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF2563EB) else Color(0xFF94A3B8))
                )

                Spacer(modifier = Modifier.width(9.dp))

                // 狀態文字
                Text(
                    text = if (isRunning) "ENGINE ACTIVE" else "ENGINE STANDBY",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) Color(0xFF0F172A) else Color(0xFF64748B),
                    letterSpacing = 1.0.sp
                )

                // 運轉時，銜接分隔點與計時數字
                if (isRunning && uptimeFormatted.isNotBlank()) {
                    Text(
                        text = "  ·  ",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    Text(
                        text = uptimeFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 副標題：Server URL（字串絕對置中，複製 icon 置於右側，無輸入框、無圓點）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // 水平絕對置中的 URL 字串
                Text(
                    text = endpointUrl,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.3.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(endpointUrl))
                            isCopied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // 放置於右側的複製 icon 按鈕
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(endpointUrl))
                        isCopied = true
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                ) {
                    Crossfade(
                        targetState = isCopied,
                        animationSpec = tween(durationMillis = 200),
                        label = "CopyIconCrossfade"
                    ) { copied ->
                        if (copied) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已複製",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "複製端點",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
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
