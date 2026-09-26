package com.geminiflow.app.presentation.features.playground

import com.geminiflow.app.domain.model.chat.PlaygroundChatMessage

data class PlaygroundUiState(
    val selectedModel: String = "gemini-3-pro",
    val promptInput: String = "請用繁體中文自我介紹，並告訴我你支援什麼功能。",
    val playgroundMessages: List<PlaygroundChatMessage> = emptyList(),
    val isGenerating: Boolean = false
)
