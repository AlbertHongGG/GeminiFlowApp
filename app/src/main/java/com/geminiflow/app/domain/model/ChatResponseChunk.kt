package com.geminiflow.app.domain.model

data class ChatResponseChunk(
    val text: String? = null,
    val imageUrl: String? = null,
    var imageLocalPath: String? = null,
    val sessionIds: List<String>? = null
)
