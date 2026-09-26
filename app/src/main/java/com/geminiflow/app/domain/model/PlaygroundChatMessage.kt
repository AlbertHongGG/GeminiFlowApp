package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * 訊息傳遞與生成狀態生命週期枚舉。
 */
enum class MessageDeliveryState {
    PENDING,
    STREAMING,
    SUCCESS,
    FAILED
}

/**
 * 沙盒對話訊息項目領域模型。
 * 採用強型別 mediaAssets 狀態機，徹底消除原始型別偏執。
 */
data class PlaygroundChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val mediaAssets: List<MediaAsset> = emptyList(),
    val isStreaming: Boolean = false,
    val deliveryState: MessageDeliveryState = if (isStreaming) MessageDeliveryState.STREAMING else MessageDeliveryState.SUCCESS,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
