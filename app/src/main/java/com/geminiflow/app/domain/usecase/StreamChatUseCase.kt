package com.geminiflow.app.domain.usecase

import android.util.Log
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.ChatResponseChunk
import com.geminiflow.app.domain.model.SessionData
import com.geminiflow.app.domain.model.TokenExpiredException
import com.geminiflow.app.domain.repository.GeminiAuthRepository
import com.geminiflow.app.domain.repository.GeminiChatRepository
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StreamChatUseCase(
    private val authRepository: GeminiAuthRepository,
    private val chatRepository: GeminiChatRepository,
    private val sessionRepository: SessionRepository,
    private val imageRepository: ImageRepository
) {
    companion object {
        private const val TAG = "StreamChatUseCase"
    }

    suspend operator fun invoke(request: ChatRequest): Flow<ChatResponseChunk> = flow {
        val sessionData = if (!request.sessionId.isNullOrBlank()) {
            sessionRepository.getSession(request.sessionId)
        } else null

        var tokens = authRepository.ensureValidTokens(forceRefresh = false)
        var cookies = authRepository.getGoogleCookies()

        try {
            executeChatStream(request, tokens, cookies, sessionData).collect { chunk ->
                emit(chunk)
            }
        } catch (e: TokenExpiredException) {
            if (!request.autoRefreshCookies) {
                throw e
            }
            Log.i(TAG, "Token expired during chat stream. Forcing token refresh and retrying...")
            tokens = authRepository.ensureValidTokens(forceRefresh = true)
            cookies = authRepository.getGoogleCookies()

            executeChatStream(request, tokens, cookies, sessionData).collect { chunk ->
                emit(chunk)
            }
        }
    }

    private suspend fun executeChatStream(
        request: ChatRequest,
        tokens: com.geminiflow.app.domain.model.GeminiTokens,
        cookies: Map<String, String>,
        sessionData: SessionData?
    ): Flow<ChatResponseChunk> = flow {
        chatRepository.streamGenerate(request, tokens, cookies, sessionData).collect { chunk ->
            if (chunk.sessionIds != null && !request.sessionId.isNullOrBlank()) {
                val newSession = SessionData(
                    sessionId = request.sessionId,
                    conversationIds = chunk.sessionIds
                )
                sessionRepository.saveSession(newSession)
            }

            if (!chunk.imageUrl.isNullOrBlank()) {
                var imgUrl = chunk.imageUrl
                if (imgUrl.contains("googleusercontent.com")) {
                    val parts = imgUrl.split("?", limit = 2)
                    var base = parts[0]
                    base = if (base.contains("=")) {
                        base.replace(Regex("=[^=]*$"), "=s0-d")
                    } else {
                        "$base=s0-d"
                    }
                    imgUrl = if (parts.size > 1) "$base?${parts[1]}" else base
                }

                try {
                    val downloadedFile = imageRepository.downloadImage(imgUrl, request.model)
                    chunk.imageLocalPath = downloadedFile.name
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download generated image: ${e.message}", e)
                }
            }

            emit(chunk)
        }
    }
}
