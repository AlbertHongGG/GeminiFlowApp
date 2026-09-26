package com.geminiflow.app.domain.usecase

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.repository.AuthRepository

class EnsureAuthUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): GeminiTokens {
        return authRepository.ensureValidTokens(forceRefresh)
    }
}
