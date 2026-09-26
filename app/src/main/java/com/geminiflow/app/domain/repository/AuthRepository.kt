package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.GeminiTokens
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun ensureValidTokens(forceRefresh: Boolean = false): GeminiTokens
    suspend fun getGoogleCookies(): Map<String, String>
    suspend fun saveCookies(cookiesHeader: String)
    suspend fun clearAuth()
}
