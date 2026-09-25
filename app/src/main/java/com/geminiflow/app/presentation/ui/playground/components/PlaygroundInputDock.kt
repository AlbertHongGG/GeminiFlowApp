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
 * 旗艦 Gemini 原生風格完整下半部輸入面板 (Gemini-Style Full Bottom Dock Panel)。
 *
 * 架構特點：
 * 1. 滿版下半部面板底板：完全貼合左右螢幕邊緣 (0dp 水平邊界)，頂部具備 28dp 大圓角與精緻陰影，完美對標 Google Gemini 原生版面。
 * 2. 第一列 (Row 1)：全寬多行文字輸入區，舒適行高與自適應高度。
 * 3. 第二列 (Row 2)：工具與控制列，左側 [+] 與 [Tune]，右側模型膠囊 (Model Tag) 與發送按鈕統一高度 (40dp)，呈現極致對稱的美學比例。
 * 4. 軟鍵盤與導航適配：鍵盤收起時白底板無縫延展至螢幕底緣，鍵盤彈起時整座面板流暢懸貼於鍵盤上方。
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.03f)
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = AppColors.surfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
        ) {
            // ==================== 第一列 (Row 1): 多行文字輸入列 ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (input.isEmpty()) {
                    Text(
                        text = "問問 GeminiFlow...",
                        fontSize = 16.sp,
                        color = AppColors.textSecondaryLight,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )
                }

                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = AppColors.textPrimaryLight,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    cursorBrush = SolidColor(AppColors.primary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==================== 第二列 (Row 2): 功能工具列 (高度嚴格統一為 40dp) ====================
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
                            .size(40.dp)
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
                            .size(40.dp)
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

                // 右側操作群組 (模型切換膠囊 與 發送按鈕，高度嚴格一致 40dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gemini 原生風格模型切換膠囊 (高度 40dp，與發送按鈕完全同高)
                    Surface(
                        onClick = onOpenModelSheet,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentModelSpec.shortName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // 動態操作按鈕 (高度直徑 40dp，與模型膠囊完全同高)
                    if (isGenerating) {
                        IconButton(
                            onClick = onCancelGeneration,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColors.danger)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "中止生成",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        val canSend = input.isNotBlank()
                        IconButton(
                            onClick = onSend,
                            enabled = canSend,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (canSend) Color(0xFFEFF4FA) else Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "發送",
                                tint = if (canSend) Color(0xFF1E293B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
