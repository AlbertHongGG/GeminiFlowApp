package com.geminiflow.app.presentation.features.playground

import com.geminiflow.app.domain.model.chat.ModelFamily
import com.geminiflow.app.domain.model.chat.PlaygroundChatMessage
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import com.geminiflow.app.presentation.features.playground.model.PlaygroundModelSpec

data class PlaygroundUiState(
    val selectedFamily: ModelFamily = ModelFamily.FLASH,
    val selectedThinking: ThinkingLevel = ThinkingLevel.LOW,
    val promptInput: String = "請用繁體中文自我介紹，並告訴我你支援什麼功能。",
    val playgroundMessages: List<PlaygroundChatMessage> = emptyList(),
    val isGenerating: Boolean = false
) {
    val selectedModel: String
        get() = PlaygroundModelSpec.findByFamily(selectedFamily).id
}
