package com.geminiflow.app.presentation.features.playground.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import com.geminiflow.app.presentation.features.playground.model.PlaygroundModelSpec
import com.geminiflow.app.presentation.theme.AppColors

@Composable
fun PlaygroundInputDock(
    input: String,
    onInputChange: (String) -> Unit,
    currentModelSpec: PlaygroundModelSpec,
    currentThinking: ThinkingLevel = ThinkingLevel.LOW,
    selectedImages: List<GalleryMediaItem> = emptyList(),
    onRemoveImage: (GalleryMediaItem) -> Unit = {},
    isGenerating: Boolean,
    onSend: () -> Unit,
    onCancelGeneration: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenGallery: () -> Unit,
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
            // 已選取圖片預覽列 (橫向滾動縮圖卡片，帶右上角刪除按鈕)
            AnimatedVisibility(
                visible = selectedImages.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedImages, key = { it.id }) { item ->
                        val context = LocalContext.current
                        val imageRequest = remember(item.uri) {
                            ImageRequest.Builder(context)
                                .data(item.uri)
                                .size(180, 180)
                                .crossfade(false)
                                .build()
                        }
                        Box(
                            modifier = Modifier.size(60.dp)
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            )

                            // 右上角微型圓形移除按鈕
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(18.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .clickable { onRemoveImage(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "移除圖片",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (input.isEmpty()) {
                    Text(
                        text = if (selectedImages.isNotEmpty()) "為相片補充問題... (直接按發送也能分析)" else "問問 GeminiFlow...",
                        fontSize = 15.sp,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側功能按鈕區：範本按鈕 (+) 與相簿選擇按鈕 (相片圖示)
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
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenGallery,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "選擇相片",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenModelSheet,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pillText = when (currentThinking) {
                                ThinkingLevel.LOW -> currentModelSpec.shortName
                                ThinkingLevel.MEDIUM -> "${currentModelSpec.shortName} · 中"
                                ThinkingLevel.HIGH -> "${currentModelSpec.shortName} · 高"
                            }
                            Text(
                                text = pillText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

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
                        val canSend = input.isNotBlank() || selectedImages.isNotEmpty()
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
