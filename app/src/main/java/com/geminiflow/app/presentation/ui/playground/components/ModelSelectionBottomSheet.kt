package com.geminiflow.app.presentation.ui.playground.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.theme.AppColors
import com.geminiflow.app.presentation.ui.playground.model.PlaygroundModelSpec

/**
 * 緊湊型專業模型選擇抽屜 (Clean & Zero-fluff)。
 * 捨棄臃腫的大卡片與行銷贅言，單頁一覽全貌，零滾動干擾，點選即用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ImmersiveBottomSheet(
        title = "選擇模型",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlaygroundModelSpec.AVAILABLE_MODELS.forEach { model ->
                val isSelected = model.id == selectedModelId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onModelSelected(model.id)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFF1F5F9) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 模型名稱
                        Text(
                            text = model.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = AppColors.textPrimaryLight
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // 能力簡約 Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = model.tag,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 選中指示圖標
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "已選中" else "未選中",
                            tint = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
