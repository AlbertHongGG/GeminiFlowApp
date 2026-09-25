package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 電池最佳化設定底部抽屜，依據系統電源狀態提供雙狀態指引。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideBottomSheet(
    isUnrestricted: Boolean,
    onRequestUnrestricted: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImmersiveBottomSheet(
        title = "電池最佳化設定",
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (isUnrestricted) {
            BatteryUnrestrictedContent(
                onOpenBatterySettings = onOpenBatterySettings
            )
        } else {
            BatteryRestrictedContent(
                onRequestUnrestricted = onRequestUnrestricted,
                onOpenBatterySettings = onOpenBatterySettings
            )
        }
    }
}

/**
 * 已設定為無限制時的狀態呈現內容。
 */
@Composable
private fun BatteryUnrestrictedContent(
    onOpenBatterySettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 翡翠綠成功狀態卡片
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "已設置為無限制",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "背景監聽服務處於最佳長效運行狀態",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 重點說明
        Text(
            text = "系統已允許 GeminiFlow 保持後台連線，手機休眠與鎖定螢幕時，本機 AI 服務將穩定響應請求不中斷。",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 輔助操作按鈕
        GfButton(
            text = "檢視系統電池設定",
            onClick = onOpenBatterySettings,
            variant = GfButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 尚未設定（受系統限制）時的狀態呈現內容。
 */
@Composable
private fun BatteryRestrictedContent(
    onRequestUnrestricted: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 琥珀色警示狀態卡片
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryAlert,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "背景執行受系統限制",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "休眠時系統可能凍結或中斷本機服務",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 重點說明
        Text(
            text = "請將電池用量設定為「不受限制」，允許 GeminiFlow 在螢幕鎖定與休眠狀態下持續接收並代理 API 請求。",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 核心主按鈕
        GfButton(
            text = "立即設定為無限制",
            onClick = onRequestUnrestricted,
            variant = GfButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 次要管道
        TextButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "手動開啟系統電池設定",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}
