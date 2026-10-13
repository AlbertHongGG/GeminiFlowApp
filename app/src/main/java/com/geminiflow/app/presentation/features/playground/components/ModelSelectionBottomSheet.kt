package com.geminiflow.app.presentation.features.playground.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.geminiflow.app.domain.model.chat.ModelFamily
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import com.geminiflow.app.presentation.components.ImmersiveBottomSheet
import com.geminiflow.app.presentation.features.playground.model.PlaygroundModelSpec
import com.geminiflow.app.presentation.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
    selectedFamily: ModelFamily,
    selectedThinking: ThinkingLevel = ThinkingLevel.LOW,
    onFamilySelected: (ModelFamily) -> Unit,
    onThinkingSelected: (ThinkingLevel) -> Unit,
    onDismiss: () -> Unit
) {
    ImmersiveBottomSheet(
        title = "選擇模型",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlaygroundModelSpec.AVAILABLE_MODELS.forEach { model ->
                val isSelected = model.family == selectedFamily

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onFamilySelected(model.family)
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
                        Text(
                            text = model.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = AppColors.textPrimaryLight
                        )

                        Spacer(modifier = Modifier.width(10.dp))

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

                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "已選中" else "未選中",
                            tint = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "思考強度",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondaryLight,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 水平三段式分段膠囊按鈕 (Segmented Control)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val thinkingOptions = listOf(
                    ThinkingLevel.LOW to "低",
                    ThinkingLevel.MEDIUM to "中",
                    ThinkingLevel.HIGH to "高"
                )

                thinkingOptions.forEach { (level, label) ->
                    val isLevelSelected = level == selectedThinking

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onThinkingSelected(level)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLevelSelected) Color.White else Color.Transparent,
                        shadowElevation = if (isLevelSelected) 1.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isLevelSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLevelSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}
