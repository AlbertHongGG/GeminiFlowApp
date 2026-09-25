package com.geminiflow.app.presentation.ui.playground.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors
import com.geminiflow.app.presentation.ui.playground.model.PlaygroundModelSpec

/**
 * 旗艦 Gemini 風格雙層對話輸入塢組件 (Gemini-Style Two-Row Input Dock)。
 *
 * 架構規範：
 * - 根容器：外層大圓角 (26dp) 精緻陰影底板，動態適配軟鍵盤與系統導航邊距。
 * - 第一列 (Row 1)：全寬多行自適應文字輸入框，零雜訊框線，提供極致流暢輸入空間。
 * - 第二列 (Row 2)：專業工具與操作列，左側配置擴充 [+] 與範本調校 [Tune]；右側整合模型切換膠囊與高對比發送按鈕。
 */
@Composable
fun PlaygroundInputDock(
    input: String,
    onInputChange: (String) -> Unit,
    currentModelSpec: PlaygroundModelSpec,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                ),
            shape = RoundedCornerShape(26.dp),
            color = AppColors.surfaceLight,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // ==================== 第一列 (Row 1): 多行文字輸入列 ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = "問問 GeminiFlow...",
                            fontSize = 15.sp,
                            color = AppColors.textSecondaryLight,
                            modifier = Modifier.padding(start = 2.dp, top = 1.dp)
                        )
                    }

                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = AppColors.textPrimaryLight,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        cursorBrush = SolidColor(AppColors.primary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== 第二列 (Row 2): 功能與工具列 ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左側工具群組 (擴充 [+] 與 提示詞調校 [Tune])
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onOpenTemplates,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "擴充工具與範本",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenTemplates,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "提示詞範本庫與設定",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 右側操作群組 (模型切換膠囊 + 發送/中止按鈕)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gemini 風格模型切換膠囊 (點擊直接呼出模型抽屜)
                        Surface(
                            onClick = onOpenModelSheet,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentModelSpec.shortName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        // 動態操作按鈕 (生成中為中止，非生成時為發送按鈕)
                        if (isGenerating) {
                            IconButton(
                                onClick = onCancelGeneration,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.danger)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "中止生成",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            val canSend = input.isNotBlank()
                            IconButton(
                                onClick = onSend,
                                enabled = canSend,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "發送",
                                    tint = if (canSend) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部系統導航列安全留白
        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
