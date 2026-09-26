package com.geminiflow.app.domain.model.chat

data class ChatResponseChunk(
    val text: String? = null,
    val imageUrl: String? = null,
    val mediaAsset: MediaAsset? = null,
    val sessionIds: List<String>? = null
)
