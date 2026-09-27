package com.geminiflow.app.presentation.features.server

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// 右側動態視覺槽之統一直徑規範（確保三列完全對齊，絕無寬度不一）
private val RibbonVisualWidth = 110.dp
private val RibbonVisualHeight = 28.dp

/**
 * 水平流體數據軸卡片（Telemetry Ribbons Card）
 * 徹底揚棄 2x2 方塊拼裝，將請求總量、並發連線與圖床快取重構為高階整齊的水平資訊流。
 * 右側三大動態組件採嚴格統一尺寸（110dp x 28dp），視覺邊界完全垂直對齊。
 */
@Composable
fun TelemetryRibbonsCard(
    isRunning: Boolean,
    totalRequests: Long,
    activeConnections: Int,
    cacheSizeFormatted: String,
    cacheFilesCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RibbonsTransition")

    // 折線與頻譜波形相位（2.6 秒）
    val ribbonPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RibbonPhase"
    )

    // 快取槽金屬微光掠過動畫（3.6 秒）
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RibbonShimmer"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x0A0F172A),
                ambientColor = Color(0x040F172A)
            ),
        shape = RoundedCornerShape(22.dp),
        color = AzureTheme.cardSurface,
        border = BorderStroke(1.2.dp, AzureTheme.subtleBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // 標籤條
            Text(
                text = "即時遙測 TELEMETRY RIBBONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AzureTheme.textMuted,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 數據列 1：累計處理請求
            RequestsRibbonRow(
                isRunning = isRunning,
                totalRequests = totalRequests,
                ribbonPhase = ribbonPhase
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = AzureTheme.borderHairline)
            Spacer(modifier = Modifier.height(14.dp))

            // 數據列 2：活躍並發通道
            SocketsRibbonRow(
                isRunning = isRunning,
                activeConnections = activeConnections,
                ribbonPhase = ribbonPhase
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = AzureTheme.borderHairline)
            Spacer(modifier = Modifier.height(14.dp))

            // 數據列 3：本機圖床快取
            CacheRibbonRow(
                cacheSizeFormatted = cacheSizeFormatted,
                cacheFilesCount = cacheFilesCount,
                shimmerPhase = shimmerPhase
            )
        }
    }
}

/**
 * 數據列 1：累計處理請求（右側統一 110dp 寬度）
 */
@Composable
private fun RequestsRibbonRow(
    isRunning: Boolean,
    totalRequests: Long,
    ribbonPhase: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "累計處理請求",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AzureTheme.textMuted,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$totalRequests",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AzureTheme.textHeading,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "REQS",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AzureTheme.textDim
                )
            }
        }

        // 右側貝茲微折線（嚴格統一 110dp x 28dp）
        Box(
            modifier = Modifier
                .width(RibbonVisualWidth)
                .height(RibbonVisualHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(AzureTheme.slotBackgroundSubtle)
                .drawWithCache {
                    val width = size.width
                    val height = size.height
                    val midY = height / 2f
                    val strokePath = Path()
                    val fillPath = Path()

                    val strokeColor = AzureTheme.azurePrimary
                    val fillColor = AzureTheme.azurePrimary.copy(alpha = 0.12f)

                    onDrawBehind {
                        if (isRunning) {
                            strokePath.reset()
                            fillPath.reset()

                            fillPath.moveTo(0f, height)
                            strokePath.moveTo(0f, midY)
                            fillPath.lineTo(0f, midY)

                            val step = 3f
                            var x = 0f
                            var lastY = midY

                            while (x <= width) {
                                val normX = x / width
                                val wave = sin((normX * 4.2 * PI) + ribbonPhase).toFloat()
                                val y = midY + (wave * (height * 0.35f))
                                strokePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                                lastY = y
                                x += step
                            }

                            fillPath.lineTo(width, height)
                            fillPath.close()

                            drawPath(path = fillPath, color = fillColor)
                            drawPath(
                                path = strokePath,
                                color = strokeColor,
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawCircle(
                                color = strokeColor,
                                radius = 2.8.dp.toPx(),
                                center = Offset(width - 2.dp.toPx(), lastY)
                            )
                        } else {
                            drawLine(
                                color = AzureTheme.borderSubtle,
                                start = Offset(0f, midY),
                                end = Offset(width, midY),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawCircle(
                                color = AzureTheme.textDim,
                                radius = 2.dp.toPx(),
                                center = Offset(width - 2.dp.toPx(), midY)
                            )
                        }
                    }
                }
        )
    }
}

/**
 * 數據列 2：活躍並發通道（右側統一 110dp 寬度）
 */
@Composable
private fun SocketsRibbonRow(
    isRunning: Boolean,
    activeConnections: Int,
    ribbonPhase: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "活躍並發通道",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AzureTheme.textMuted,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$activeConnections",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AzureTheme.textHeading,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "SOCKETS",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AzureTheme.textDim
                )
            }
        }

        // 右側 8 階動態湛藍頻譜柱（嚴格統一 110dp x 28dp）
        Box(
            modifier = Modifier
                .width(RibbonVisualWidth)
                .height(RibbonVisualHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(AzureTheme.slotBackgroundSubtle),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val totalBars = 8
                for (i in 0 until totalBars) {
                    val barFraction = if (isRunning) {
                        val base = when {
                            activeConnections > 0 -> (activeConnections.toFloat() / 8f).coerceIn(0.25f, 1f)
                            else -> 0.16f
                        }
                        val offset = (i * 0.45f)
                        val wave = (sin(ribbonPhase + offset) + 1f) / 2f
                        (base * 0.5f + wave * 0.5f).coerceIn(0.12f, 1f)
                    } else {
                        0.1f
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.5.dp)
                            .fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(
                                if (isRunning) {
                                    if (i < activeConnections.coerceAtLeast(1)) {
                                        AzureTheme.azureDeep
                                    } else {
                                        AzureTheme.azureLight.copy(alpha = 0.7f)
                                    }
                                } else {
                                    AzureTheme.borderSubtle
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * 數據列 3：本機圖床快取（右側統一 110dp 寬度）
 */
@Composable
private fun CacheRibbonRow(
    cacheSizeFormatted: String,
    cacheFilesCount: Int,
    shimmerPhase: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "本機圖床快取",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AzureTheme.textMuted,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = cacheSizeFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AzureTheme.textHeading,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($cacheFilesCount 圖檔)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AzureTheme.textMuted
                )
            }
        }

        // 右側晶透高光進度槽（嚴格統一 110dp x 28dp 外層槽，內嵌居中進度條）
        Box(
            modifier = Modifier
                .width(RibbonVisualWidth)
                .height(RibbonVisualHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(AzureTheme.slotBackgroundSubtle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AzureTheme.slotBackground)
            ) {
                val progressFraction = when {
                    cacheFilesCount > 20 -> 0.9f
                    cacheFilesCount > 0 -> (0.2f + (cacheFilesCount * 0.035f)).coerceAtMost(0.85f)
                    else -> 0.08f
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width * progressFraction
                    val barHeight = size.height

                    // 星鑽湛藍漸層進度條
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(AzureTheme.azureDeep, AzureTheme.azureGlow)
                        ),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // 微光金屬反射掠過
                    if (cacheFilesCount > 0) {
                        val shimmerX = barWidth * shimmerPhase
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.65f),
                                    Color.Transparent
                                ),
                                startX = shimmerX - 10.dp.toPx(),
                                endX = shimmerX + 10.dp.toPx()
                            ),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
