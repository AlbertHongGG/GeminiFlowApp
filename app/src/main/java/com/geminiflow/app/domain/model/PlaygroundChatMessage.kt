package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * 沙盒對話訊息項目資料模型。
 */
data class PlaygroundChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val images: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
