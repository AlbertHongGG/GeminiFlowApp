package com.geminiflow.app.presentation.ui.server

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.notification.NotificationController
import com.geminiflow.app.presentation.theme.AppColors
import kotlin.math.sin

/**
 * 伺服器引擎核心主體控制台 (EngineCoreVisualizer 旗艦解耦版)。
 *
 * 架構特點：
 * 1. 向量圖形 100% 毫無遮擋：中段獨立展開即時流體示波儀 (Flow Stream Wave Canvas)，線條細節清澈可見。
 * 2. 極簡微狀態燈：徹底拔除臃腫厚重的 Tag 泡泡與同義贅言，以 6dp 呼吸光點 + 純粹狀態文字呈現。
 * 3. 垂直節奏緊湊：總高度精準控制在 210dp 內，為底層遙測指標釋放充足空間，避免被浮動導航欄遮蔽。
 */
@Composable
fun EngineCoreVisualizer(
    isRunning: Boolean,
    host: String,
    port: Int,
    errorMessage: String?,
    onToggleServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val endpointUrl = "http://$host:$port"

    val infiniteTransition = rememberInfiniteTransition(label = "FlowWaveTransition")

    // 運行狀態下的平滑波形移動相位
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // 狀態指示燈微呼吸動畫
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.04f),
                ambientColor = Color.Black.copy(alpha = 0.02f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.surfaceLight,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // 1. 頂部狀態列：左側標題 + 右側極簡微狀態燈 (無任何厚重背景框干擾)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "伺服器引擎",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimaryLight,
                    letterSpacing = 0.3.sp
                )

                // 極簡微狀態燈 (無多餘臃腫氣泡，僅光點 + 清晰文字)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) Color(0xFF10B981).copy(alpha = pulseAlpha)
                                else Color(0xFF94A3B8)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "運作中" else "已停止",
                        fontSize = 12.sp,
                        fontWeight = if (isRunning) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isRunning) Color(0xFF059669) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 獨立即時流體示波儀 (100% 無任何遮蔽，幾何線條完整展現)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                if (isRunning) {
                    // (A) 主資料流動微波 (翡翠綠柔滑雙層諧波)
                    val primaryPath = Path()
                    val secondaryPath = Path()
                    val step = 3f
                    var x = 0f

                    primaryPath.moveTo(0f, midY)
                    secondaryPath.moveTo(0f, midY)

                    while (x <= width) {
                        val normX = x / width
                        val wave1 = sin((normX * 3.5 * Math.PI) + wavePhase).toFloat()
                        val wave2 = sin((normX * 5.0 * Math.PI) - wavePhase * 0.7f).toFloat()

                        primaryPath.lineTo(x, midY + (wave1 * (height * 0.36f)))
                        secondaryPath.lineTo(x, midY + (wave2 * (height * 0.20f)))
                        x += step
                    }

                    // 繪製次級微波 (半透明增加深度)
                    drawPath(
                        path = secondaryPath,
                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // 繪製主波形
                    drawPath(
                        path = primaryPath,
                        color = Color(0xFF10B981).copy(alpha = 0.85f),
                        style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 兩側微型工程刻度標籤
                    drawLine(Color(0xFF10B981).copy(alpha = 0.5f), Offset(0f, midY - 8.dp.toPx()), Offset(0f, midY + 8.dp.toPx()), 1.5.dp.toPx())
                    drawLine(Color(0xFF10B981).copy(alpha = 0.5f), Offset(width, midY - 8.dp.toPx()), Offset(width, midY + 8.dp.toPx()), 1.5.dp.toPx())
                } else {
                    // 待命時：靜態極簡工程基準線與精密座標短線
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, midY),
                        end = Offset(width, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    // 中央十字刻度標記
                    val chLen = 5.dp.toPx()
                    drawLine(Color(0xFFCBD5E1), Offset(width / 2f, midY - chLen), Offset(width / 2f, midY + chLen), 1.2.dp.toPx())
                    drawLine(Color(0xFFCBD5E1), Offset(0f, midY - 4.dp.toPx()), Offset(0f, midY + 4.dp.toPx()), 1.dp.toPx())
                    drawLine(Color(0xFFCBD5E1), Offset(width, midY - 4.dp.toPx()), Offset(width, midY + 4.dp.toPx()), 1.dp.toPx())
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 乾淨端點晶片 (垂直順暢排列在示波儀下方，不造成任何遮擋)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboardManager.setText(AnnotatedString(endpointUrl))
                        NotificationController.showSuccess("已複製服務端點網址")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = endpointUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight,
                        letterSpacing = 0.3.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "點擊複製",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "複製端點",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 主控電源按鈕 (Master Power Switch - 緊湊高度 46dp)
            Button(
                onClick = onToggleServer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFF1E293B) else Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFFF43F5E) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "停止本地服務" else "啟動本地伺服器",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isRunning) Color(0xFFFDA4AF) else Color.White
                    )
                }
            }

            // 異常錯誤提示 (若有錯誤即時展現)
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "系統異常：$errorMessage",
                        color = Color(0xFFBE123C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
