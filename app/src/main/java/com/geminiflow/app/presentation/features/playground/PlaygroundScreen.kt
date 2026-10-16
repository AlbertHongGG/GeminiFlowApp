package com.geminiflow.app.presentation.features.playground

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import com.geminiflow.app.presentation.components.ImmersiveScaffold
import com.geminiflow.app.presentation.components.gallery.GalleryPickerBottomSheet
import com.geminiflow.app.presentation.features.playground.components.ModelSelectionBottomSheet
import com.geminiflow.app.presentation.features.playground.components.PlaygroundChatBubble
import com.geminiflow.app.presentation.features.playground.components.PlaygroundInputDock
import com.geminiflow.app.presentation.features.playground.components.TemplateSelectionBottomSheet
import com.geminiflow.app.presentation.features.playground.components.rememberPlaygroundScrollState
import com.geminiflow.app.presentation.features.playground.model.PlaygroundModelSpec
import com.geminiflow.app.presentation.theme.AppColors

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(
    viewModel: PlaygroundViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scrollState = rememberPlaygroundScrollState(lazyListState = listState)

    var showModelSheet by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showGallerySheet by remember { mutableStateOf(false) }

    val currentModelSpec = remember(uiState.selectedFamily) {
        PlaygroundModelSpec.findByFamily(uiState.selectedFamily)
    }

    // 訊息數量或圖片數量改變時，立即貼底
    val lastMessage = uiState.playgroundMessages.lastOrNull()
    LaunchedEffect(uiState.playgroundMessages.size, lastMessage?.mediaAssets?.size) {
        if (uiState.playgroundMessages.isNotEmpty()) {
            scrollState.scrollToBottomImmediate()
        }
    }

    // 串流生成期間：以低頻節流（每 100ms）維持貼底，徹底消除逐字高頻 scrollToItem 引發的 Layout Thrashing
    LaunchedEffect(lastMessage?.id, lastMessage?.isStreaming) {
        if (lastMessage?.isStreaming == true) {
            while (isActive) {
                if (scrollState.autoScrollEnabled && !listState.isScrollInProgress) {
                    scrollState.scrollToBottomImmediate()
                }
                delay(100L)
            }
        } else if (uiState.playgroundMessages.isNotEmpty()) {
            scrollState.scrollToBottomImmediate()
        }
    }

    // 監聽視口高度變更（如軟鍵盤彈出或收起）：若貼底中，維持釘在最新輸出
    LaunchedEffect(scrollState) {
        snapshotFlow { listState.layoutInfo.viewportSize }
            .distinctUntilChanged()
            .collect {
                if (scrollState.autoScrollEnabled && uiState.playgroundMessages.isNotEmpty()) {
                    scrollState.scrollToBottomImmediate()
                }
            }
    }

    if (showModelSheet) {
        ModelSelectionBottomSheet(
            selectedFamily = uiState.selectedFamily,
            selectedThinking = uiState.selectedThinking,
            onFamilySelected = { viewModel.selectFamily(it) },
            onThinkingSelected = { viewModel.selectThinking(it) },
            onDismiss = { showModelSheet = false }
        )
    }

    if (showTemplateSheet) {
        TemplateSelectionBottomSheet(
            onTemplateSelected = { prompt ->
                viewModel.updatePromptInput(prompt)
            },
            onDismiss = { showTemplateSheet = false }
        )
    }

    if (showGallerySheet) {
        GalleryPickerBottomSheet(
            onDismiss = { showGallerySheet = false },
            onConfirm = { items ->
                viewModel.addSelectedImages(items)
            }
        )
    }

    ImmersiveScaffold {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.playgroundMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 230.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = Color(0xFF0F172A).copy(alpha = 0.06f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "準備好開始測試了嗎？",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "開始連續對話與生圖測試",
                        fontSize = 13.sp,
                        color = AppColors.textSecondaryLight
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(28.dp))

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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.textPrimaryLight,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AI CHAT & IMAGE PLAYGROUND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textSecondaryLight,
                            letterSpacing = 1.5.sp
                        )
                    }

                    if (uiState.playgroundMessages.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.clearPlaygroundChat()
                                scrollState.reset()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "清空對話",
                                tint = AppColors.textSecondaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (uiState.playgroundMessages.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.playgroundMessages, key = { it.id }) { message ->
                                PlaygroundChatBubble(
                                    message = message,
                                    onRetryMedia = { assetId -> viewModel.retryMediaDownload(message.id, assetId) }
                                )
                            }
                            item(key = "bottom_anchor") {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        // 懸浮「回到底部」微型控制按鈕：離開底部時平滑浮現
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !scrollState.isAtBottom,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                        ) {
                            Surface(
                                onClick = { scrollState.requestScrollToBottom() },
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "回到底部",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                PlaygroundInputDock(
                    input = uiState.promptInput,
                    onInputChange = { viewModel.updatePromptInput(it) },
                    currentModelSpec = currentModelSpec,
                    currentThinking = uiState.selectedThinking,
                    selectedImages = uiState.selectedImages,
                    onRemoveImage = { viewModel.removeSelectedImage(it) },
                    isGenerating = uiState.isGenerating,
                    onSend = {
                        viewModel.sendPlaygroundPrompt()
                        scrollState.onUserSendMessage()
                    },
                    onCancelGeneration = { viewModel.cancelPlaygroundGeneration() },
                    onOpenModelSheet = { showModelSheet = true },
                    onOpenTemplates = { showTemplateSheet = true },
                    onOpenGallery = { showGallerySheet = true }
                )
            }
        }
    }
}
