package com.geminiflow.app.data.api.payload

import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.GeminiTokens

object PayloadBuilderFactory {
    fun create(
        request: ChatRequest,
        tokens: GeminiTokens,
        uploads: List<Pair<String, String>> = emptyList(),
        conversationIds: List<String> = emptyList()
    ): BasePayloadBuilder {
        val model = request.model.lowercase()
        return if (model.contains("pro") || model.contains("flash")) {
            ProModelBuilder(request, tokens, uploads, conversationIds)
        } else {
            StandardModelBuilder(request, tokens, uploads, conversationIds)
        }
    }
}
