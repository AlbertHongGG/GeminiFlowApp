package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet

/**
 * 電池最佳化設定底部抽屜，採用極簡置中英雄視覺呈現當前系統電源權限。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideBottomSheet(
    isUnrestricted: Boolean,
    onRequestUnrestricted: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImmersiveBottomSheet(
        title = "電池最佳化",
        onDismiss = onDismiss,
        trailingAction = {
            HeaderStatusTag(isUnrestricted = isUnrestricted)
        },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 雙層環境光暈圖章
            DualRingBadge(isUnrestricted = isUnrestricted)

            Spacer(modifier = Modifier.height(22.dp))

            // 主標題
            Text(
                text = if (isUnrestricted) "已設置為無限制" else "背景執行受限制",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 副標題（無贅句、無孤字）
            Text(
                text = if (isUnrestricted) {
                    "服務允許長效常駐，休眠時維持穩定連線"
                } else {
                    "休眠時系統將凍結本機服務，連線易中斷"
                },
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (!isUnrestricted) {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onRequestUnrestricted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "立即設定為無限制",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 頂部標題列右側精緻狀態微標籤。
 */
@Composable
private fun HeaderStatusTag(isUnrestricted: Boolean) {
    val tagBg = if (isUnrestricted) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
    val tagBorder = if (isUnrestricted) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
    val dotColor = if (isUnrestricted) Color(0xFF16A34A) else Color(0xFFD97706)
    val textColor = if (isUnrestricted) Color(0xFF15803D) else Color(0xFFB45309)
    val labelText = if (isUnrestricted) "已無限制" else "受限制"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tagBg,
        border = BorderStroke(1.dp, tagBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = labelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * 雙層環境光暈圓形圖章組件。
 */
@Composable
private fun DualRingBadge(isUnrestricted: Boolean) {
    val outerColor = if (isUnrestricted) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val innerColor = if (isUnrestricted) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
    val iconTint = if (isUnrestricted) Color(0xFF16A34A) else Color(0xFFD97706)
    val iconVector = if (isUnrestricted) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryAlert

    Box(
        modifier = Modifier
            .size(76.dp)
            .background(outerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(innerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
