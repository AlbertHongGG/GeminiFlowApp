package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.GeminiTokens
import kotlinx.coroutines.flow.StateFlow

interface GeminiAuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun ensureValidTokens(forceRefresh: Boolean = false): GeminiTokens
    suspend fun getGoogleCookies(): Map<String, String>
    suspend fun saveCookies(cookiesHeader: String)
    suspend fun clearAuth()
}
