package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionData
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ChatResponseChunk
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun streamGenerate(
        request: ChatRequest,
        tokens: GeminiTokens,
        cookies: Map<String, String>,
        sessionData: SessionData?
    ): Flow<ChatResponseChunk>
}
