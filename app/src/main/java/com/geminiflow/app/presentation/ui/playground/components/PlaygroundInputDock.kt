package com.geminiflow.app.presentation.ui.playground.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geminiflow.app.presentation.theme.AppColors

/**
 * 懸浮式對話輸入塢組件。
 */
@Composable
fun PlaygroundInputDock(
    input: String,
    onInputChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit,
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
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                    ambientColor = Color.Black.copy(alpha = 0.02f)
                ),
            shape = RoundedCornerShape(22.dp),
            color = AppColors.surfaceLight,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 多行自動延展輸入框
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = "輸入訊息或生圖提示詞...",
                            fontSize = 14.sp,
                            color = AppColors.textSecondaryLight
                        )
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 快捷範本庫純圖標按鈕 (位於發送按鈕旁，純 icon 無文字)
                IconButton(
                    onClick = onOpenTemplates,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "提示詞範本庫",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 發送 / 中止操作按鈕
                if (isGenerating) {
                    IconButton(
                        onClick = onCancelGeneration,
                        modifier = Modifier
                            .size(38.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canSend) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "發送",
                            tint = if (canSend) Color.White else Color(0xFFCBD5E1),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // 預留底部懸浮膠囊導航欄的安全高度
        Spacer(modifier = Modifier.height(96.dp))
    }
}
