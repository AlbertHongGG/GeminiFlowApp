package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.ChatResponseChunk
import com.geminiflow.app.domain.model.GeminiTokens
import com.geminiflow.app.domain.model.SessionData
import kotlinx.coroutines.flow.Flow

interface GeminiChatRepository {
    suspend fun streamGenerate(
        request: ChatRequest,
        tokens: GeminiTokens,
        cookies: Map<String, String>,
        sessionData: SessionData?
    ): Flow<ChatResponseChunk>
}
