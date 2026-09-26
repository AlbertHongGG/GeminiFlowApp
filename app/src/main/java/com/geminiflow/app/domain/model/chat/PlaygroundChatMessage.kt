package com.geminiflow.app.domain.model.chat

import java.util.UUID

enum class MessageDeliveryState {
    PENDING,
    STREAMING,
    SUCCESS,
    FAILED
}

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
