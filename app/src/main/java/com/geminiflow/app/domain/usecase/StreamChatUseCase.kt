package com.geminiflow.app.domain.usecase

import android.util.Log
import com.geminiflow.app.domain.model.ChatRequest
import com.geminiflow.app.domain.model.ChatResponseChunk
import com.geminiflow.app.domain.model.MediaAsset
import com.geminiflow.app.domain.model.SessionData
import com.geminiflow.app.domain.model.TokenExpiredException
import com.geminiflow.app.domain.repository.GeminiAuthRepository
import com.geminiflow.app.domain.repository.GeminiChatRepository
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

/**
 * 串流對話業務協調 UseCase。
 * 負責金鑰/Cookie 生命週期管理、會話維持、以及多媒體資源（MediaAsset）的下載管線協調。
 * 扮演 Anti-Corruption Layer (防腐層)，將底層原始 CDN 網址轉化為強型別之 MediaAsset 生命週期狀態。
 */
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

            if (!chunk.text.isNullOrEmpty() || chunk.sessionIds != null) {
                emit(ChatResponseChunk(text = chunk.text, sessionIds = chunk.sessionIds))
            }

            if (!chunk.imageUrl.isNullOrBlank()) {
                val rawUrl = chunk.imageUrl
                val assetId = UUID.randomUUID().toString()

                // 1. 先通知 UI 進入下載中狀態（骨架屏與進度提示）
                emit(ChatResponseChunk(mediaAsset = MediaAsset.Downloading(id = assetId, rawUrl = rawUrl)))

                // 2. 透過 ImageRepository (WebkitCookieJar 3-Hop Pipeline) 進行下載
                try {
                    val downloadedFile = imageRepository.downloadImage(rawUrl, request.model)
                    Log.i(TAG, "Image successfully saved to local path: ${downloadedFile.absolutePath}")
                    // 3. 成功落地：發送 LocalReady，UI 直接從本機檔案極速渲染
                    emit(
                        ChatResponseChunk(
                            mediaAsset = MediaAsset.LocalReady(
                                id = assetId,
                                rawUrl = rawUrl,
                                localFile = downloadedFile
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading image from $rawUrl: ${e.message}", e)
                    // 4. 失敗：發送 Failed，攜帶精確錯誤訊息，杜絕空白與靜默失敗
                    emit(
                        ChatResponseChunk(
                            mediaAsset = MediaAsset.Failed(
                                id = assetId,
                                rawUrl = rawUrl,
                                errorMessage = e.message ?: "圖片下載管線異常"
                            )
                        )
                    )
                }
            }
        }
    }
}
