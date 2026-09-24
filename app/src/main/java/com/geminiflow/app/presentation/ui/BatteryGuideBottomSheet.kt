package com.geminiflow.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.GfButton
import com.geminiflow.app.presentation.components.GfButtonVariant
import com.geminiflow.app.presentation.components.GfCard
import com.geminiflow.app.presentation.theme.AccentAmber
import com.geminiflow.app.presentation.theme.AccentAmberLight
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.AccentEmerald
import com.geminiflow.app.presentation.theme.AccentEmeraldLight
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.SurfaceElevated
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary

/**
 * BatteryGuideBottomSheet: Half-screen pure light bottom sheet for battery optimization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideBottomSheet(
    isUnrestricted: Boolean,
    oemTips: String,
    onRequestUnrestricted: () -> Unit,
    onOpenOemSettings: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUnrestricted) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = if (isUnrestricted) AccentEmerald else AccentAmber,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "電池最佳化與防殺設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "確保手機休眠時服務持續在背景監聽",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Status Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUnrestricted) AccentEmeraldLight else AccentAmberLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnrestricted) AccentEmerald.copy(alpha = 0.3f) else AccentAmber.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isUnrestricted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isUnrestricted) AccentEmerald else AccentAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isUnrestricted) "目前狀態：已設置為「無限制 (Unrestricted)」" else "目前狀態：受系統限制（休眠時易遭系統關閉）",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isUnrestricted) AccentEmerald else AccentAmber
                    )
                }
            }

            // Step 1: Unrestricted Battery
            GfCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceElevated,
                borderColor = BorderLight,
                contentPadding = 14.dp
            ) {
                Text(
                    text = "步驟 1：電池最佳化設置為「無限制」",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "點擊下方按鈕以申請忽略電池最佳化，允許 App 在螢幕鎖定時保持 CPU 運行。",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                GfButton(
                    text = if (isUnrestricted) "重新檢查授權" else "一鍵設置為無限制",
                    onClick = onRequestUnrestricted,
                    variant = GfButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Step 2: OEM Auto-Start & Background Lock
            if (oemTips.isNotBlank()) {
                GfCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceElevated,
                    borderColor = BorderLight,
                    contentPadding = 14.dp
                ) {
                    Text(
                        text = "步驟 2：手機廠牌特有防殺配置",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = oemTips,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GfButton(
                        text = "前往廠牌專屬後台設置",
                        onClick = onOpenOemSettings,
                        variant = GfButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            GfButton(
                text = "完成並返回",
                onClick = onDismiss,
                variant = GfButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
