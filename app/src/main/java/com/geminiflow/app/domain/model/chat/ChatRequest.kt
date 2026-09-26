package com.geminiflow.app.domain.model.chat

data class ChatRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val model: String,
    val language: String = "zh-TW",
    val images: List<ImagePayload> = emptyList(),
    val sessionId: String? = null,
    val autoRefreshCookies: Boolean = true
)
