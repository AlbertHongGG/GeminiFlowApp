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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors
import kotlin.math.sin

/**
 * 圖形化精密遙測儀表組 (TelemetryGraphicInstrument)。
 * 告別死板文字方塊，融合 Canvas 活體波形線條、多段式 LED 計量條、微排版時鐘與儲存刻度條。
 */
@Composable
fun TelemetryGraphicInstrument(
    isRunning: Boolean,
    totalRequests: Long,
    activeConnections: Int,
    uptimeFormatted: String,
    cacheSizeFormatted: String,
    cacheFilesCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TelemetryWaveTransition")

    // 波形流動平移相位
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // 1Hz 脈動時鐘信號燈
    val clockPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ClockPulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一列：累計請求 (含 Canvas 動態火花波形) + 活躍並發 (含多段式 LED 計量條)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 累計請求儀表 (Request Throughput with Kinetic Sparkline)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surfaceLight,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "累計處理請求",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$totalRequests",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Canvas 動態火花波形 (Sparkline)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f

                        if (isRunning) {
                            val path = Path()
                            val step = 4f
                            var x = 0f
                            path.moveTo(0f, midY)

                            while (x <= width) {
                                val normalizedX = x / width
                                val wave = sin((normalizedX * 4 * Math.PI) + wavePhase).toFloat()
                                val y = midY + (wave * (height * 0.35f))
                                path.lineTo(x, y)
                                x += step
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF10B981).copy(alpha = 0.7f),
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // 待命時呈現精準基準線
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, midY),
                                end = Offset(width, midY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // 2. 活躍並發儀表 (Active Concurrency with Segmented Signal Meter)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surfaceLight,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "活躍並發連線",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$activeConnections",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5 段式微型 LED 信號計量條
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val litSegments = when {
                            activeConnections >= 5 -> 5
                            activeConnections in 1..4 -> activeConnections
                            isRunning -> 1
                            else -> 0
                        }

                        for (i in 1..5) {
                            val isLit = i <= litSegments
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(
                                        when {
                                            isLit && isRunning -> Color(0xFF0F172A)
                                            isLit -> Color(0xFF94A3B8)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 第二列：連續運行時長 (含數位時鐘微排版) + 快取佔用 (含微型進度刻度條)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3. 運行時長儀表 (Uptime Chronograph)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surfaceLight,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "服務運行時長",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = uptimeFormatted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRunning) Color(0xFF10B981).copy(alpha = clockPulse)
                                    else Color(0xFFCBD5E1)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "即時運行中" else "尚未啟動",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isRunning) Color(0xFF0F172A) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // 4. 圖床快取儀表 (Storage Footprint Gauge)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surfaceLight,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "本機圖床快取",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = cacheSizeFormatted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$cacheFilesCount 個圖檔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )

                        // 迷你微型儲存計量指示器
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            val progress = if (cacheFilesCount > 0) 0.65f else 0.05f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF0F172A))
                            )
                        }
                    }
                }
            }
        }
    }
}
