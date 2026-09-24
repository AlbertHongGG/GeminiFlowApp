package com.geminiflow.app.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.geminiflow.app.domain.model.PlaygroundChatMessage
import com.geminiflow.app.presentation.components.GfCard
import com.geminiflow.app.presentation.theme.AccentBlue
import com.geminiflow.app.presentation.theme.AccentBlueLight
import com.geminiflow.app.presentation.theme.AccentRose
import com.geminiflow.app.presentation.theme.BgCanvas
import com.geminiflow.app.presentation.theme.BorderFocused
import com.geminiflow.app.presentation.theme.BorderLight
import com.geminiflow.app.presentation.theme.SurfaceCard
import com.geminiflow.app.presentation.theme.SurfaceElevated
import com.geminiflow.app.presentation.theme.TextMuted
import com.geminiflow.app.presentation.theme.TextPrimary
import com.geminiflow.app.presentation.theme.TextSecondary
import com.geminiflow.app.presentation.viewmodel.MainViewModel

private val AVAILABLE_MODELS = listOf(
    "gemini-3-pro" to "3-Pro (旗艦生圖)",
    "gemini-3.5-flash" to "3.5-Flash (極速)",
    "gemini-3.7-flash" to "3.7-Flash (平衡)",
    "gemini-3-flash-image" to "Flash-Image (純圖)"
)

/**
 * PlaygroundScreen: Interactive AI Chat and Image Sandbox
 */
@Composable
fun PlaygroundScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(uiState.playgroundMessages.size, uiState.playgroundMessages.lastOrNull()?.text?.length) {
        if (uiState.playgroundMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.playgroundMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // 1. Headerless Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "模型沙盒",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "本機即時對話與生圖測試",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (uiState.playgroundMessages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearPlaygroundChat() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "清空對話",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Model Segmented Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AVAILABLE_MODELS.forEach { (modelId, label) ->
                val isSelected = uiState.selectedModel == modelId
                Surface(
                    modifier = Modifier.clickable { viewModel.selectModel(modelId) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AccentBlueLight else SurfaceCard,
                    border = BorderStroke(1.dp, if (isSelected) AccentBlue else BorderLight),
                    shadowElevation = if (isSelected) 0.dp else 1.dp
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AccentBlue else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Message Timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.playgroundMessages.isEmpty()) {
                // Empty state suggestions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = AccentBlue.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "準備好開始測試了嗎？",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "點擊下方範例即可直接發送提問或生圖",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    listOf(
                        "請用繁體中文自我介紹，並告訴我你支援什麼功能。",
                        "請畫一隻穿著宇航服在火星探險的柴犬，高解析度寫實風格。",
                        "請簡明扼要解釋量子運算與傳統運算的差異。"
                    ).forEach { suggestion ->
                        GfCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updatePromptInput(suggestion)
                                    viewModel.sendPlaygroundPrompt()
                                },
                            contentPadding = 12.dp
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.playgroundMessages, key = { it.id }) { message ->
                        PlaygroundBubble(message = message)
                    }
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 4. Docked Bottom Input Island
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.promptInput,
                    onValueChange = { viewModel.updatePromptInput(it) },
                    placeholder = { Text("輸入訊息或生圖提示詞...", fontSize = 14.sp, color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendPlaygroundPrompt() })
                )

                if (uiState.isGenerating) {
                    IconButton(
                        onClick = { viewModel.cancelPlaygroundGeneration() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentRose)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "中止",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.sendPlaygroundPrompt() },
                        enabled = uiState.promptInput.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (uiState.promptInput.isNotBlank()) AccentBlue else SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "發送",
                            tint = if (uiState.promptInput.isNotBlank()) Color.White else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(78.dp)) // Clearance for floating nav dock
    }
}

@Composable
private fun PlaygroundBubble(message: PlaygroundChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.95f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) AccentBlueLight else SurfaceCard,
                border = BorderStroke(1.dp, if (isUser) AccentBlue.copy(alpha = 0.25f) else BorderLight),
                shadowElevation = if (isUser) 0.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                    } else if (message.isStreaming) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在思考與生成中...", fontSize = 13.sp, color = TextSecondary)
                        }
                    }

                    // Generated Images Gallery
                    if (message.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        message.images.forEach { imagePath ->
                            AsyncImage(
                                model = imagePath,
                                contentDescription = "生成圖片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}
