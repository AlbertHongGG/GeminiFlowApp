package com.geminiflow.app.domain.usecase

import com.geminiflow.app.domain.model.GeminiTokens
import com.geminiflow.app.domain.repository.GeminiAuthRepository

class EnsureAuthUseCase(
    private val authRepository: GeminiAuthRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): GeminiTokens {
        return authRepository.ensureValidTokens(forceRefresh)
    }
}
