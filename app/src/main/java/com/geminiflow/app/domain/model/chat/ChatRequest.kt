package com.geminiflow.app.domain.model.chat

data class ChatRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val model: String = "gemini-3.8-flash",
    val thinking: ThinkingLevel = ThinkingLevel.LOW,
    val language: String = "zh-TW",
    val images: List<ImagePayload> = emptyList(),
    val sessionId: String? = null
) {
    val modelSpec: ModelSpec get() = ModelSpec.resolve(model, thinking)

    val effectivePrompt: String
        get() = if (!systemPrompt.isNullOrBlank()) {
            "System:\n$systemPrompt\n\nUser:\n$prompt"
        } else {
            prompt
        }
}
