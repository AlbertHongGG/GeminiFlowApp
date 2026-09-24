package com.geminiflow.app.domain.model

import java.util.UUID

/**
 * PlaygroundChatMessage: Individual bubble message in PlaygroundScreen
 */
data class PlaygroundChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val images: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
