package com.geminiflow.app.domain.usecase

import android.util.Log
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionData
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ChatResponseChunk
import com.geminiflow.app.domain.model.chat.MediaAsset
import com.geminiflow.app.domain.model.common.TokenExpiredException
import com.geminiflow.app.domain.repository.AuthRepository
import com.geminiflow.app.domain.repository.ChatRepository
import com.geminiflow.app.domain.repository.ImageRepository
import com.geminiflow.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class StreamChatUseCase(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
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
        tokens: GeminiTokens,
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

            if (chunk.exportMetadata != null) {
                val exportMeta = chunk.exportMetadata
                val previewUrl = exportMeta.previewUrl
                val assetId = UUID.randomUUID().toString()

                emit(ChatResponseChunk(mediaAsset = MediaAsset.Downloading(id = assetId, rawUrl = previewUrl, exportMetadata = exportMeta)))

                try {
                    val validTokens = tokens ?: throw TokenExpiredException("缺少 Google 帳號授權 Token，無法執行 c8o8Fe 原圖導出")
                    val downloadedFile = imageRepository.downloadExportedImage(
                        metadata = exportMeta,
                        modelName = request.model,
                        tokens = validTokens,
                        cookies = cookies
                    )
                    val mimeType = if (downloadedFile.extension.equals("jpg", true) || downloadedFile.extension.equals("jpeg", true)) {
                        "image/jpeg"
                    } else {
                        "image/png"
                    }
                    emit(
                        ChatResponseChunk(
                            mediaAsset = MediaAsset.LocalReady(
                                id = assetId,
                                rawUrl = previewUrl,
                                localFile = downloadedFile,
                                mimeType = mimeType
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading exported high-resolution image: ${e.message}", e)
                    emit(
                        ChatResponseChunk(
                            mediaAsset = MediaAsset.Failed(
                                id = assetId,
                                rawUrl = previewUrl,
                                errorMessage = e.message ?: "原圖導出失敗",
                                exportMetadata = exportMeta
                            )
                        )
                    )
                }
            } else if (!chunk.imageUrl.isNullOrBlank()) {
                val rawUrl = chunk.imageUrl
                val assetId = UUID.randomUUID().toString()

                emit(ChatResponseChunk(mediaAsset = MediaAsset.Downloading(id = assetId, rawUrl = rawUrl)))

                try {
                    val downloadedFile = imageRepository.downloadImage(
                        url = rawUrl,
                        modelName = request.model
                    )
                    val mimeType = if (downloadedFile.extension.equals("jpg", true) || downloadedFile.extension.equals("jpeg", true)) {
                        "image/jpeg"
                    } else {
                        "image/png"
                    }
                    emit(
                        ChatResponseChunk(
                            mediaAsset = MediaAsset.LocalReady(
                                id = assetId,
                                rawUrl = rawUrl,
                                localFile = downloadedFile,
                                mimeType = mimeType
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading image from $rawUrl: ${e.message}", e)
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
