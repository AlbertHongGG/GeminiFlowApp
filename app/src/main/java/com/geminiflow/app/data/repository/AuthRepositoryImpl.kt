package com.geminiflow.app.data.repository

import com.geminiflow.app.data.auth.CredentialStore
import com.geminiflow.app.domain.auth.TokenManager
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionState
import com.geminiflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 領域鑑權門面實作。
 * 採用組合模式 (Composition over Inheritance)，
 * 組合本機憑證庫 [CredentialStore] 與動態權杖管理員 [TokenManager]。
 *
 * 核心不變量 (Invariants)：
 * 1. [sessionState] 與 [isAuthenticated] 唯一依託於 [credentialStore.hasRequiredCredential()]。
 * 2. 任何 HTTP 請求或權杖解析異常，絕對不可被動篡改身分狀態！
 * 3. 只有在調用 [saveCookies] 或 [clearAuth] 時，身分狀態才會發生狀態躍遷。
 */
class AuthRepositoryImpl(
    private val credentialStore: CredentialStore,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val authMutex = Mutex()

    private val _sessionState = MutableStateFlow<SessionState>(
        if (credentialStore.hasRequiredCredential()) SessionState.Authenticated() else SessionState.Unauthenticated
    )
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(credentialStore.hasRequiredCredential())
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    override suspend fun getGoogleCookies(): Map<String, String> {
        return credentialStore.getCookies()
    }

    override suspend fun saveCookies(cookiesHeader: String) {
        authMutex.withLock {
            credentialStore.saveCookies(cookiesHeader)
            val hasCred = credentialStore.hasRequiredCredential()
            _sessionState.value = if (hasCred) SessionState.Authenticated() else SessionState.Unauthenticated
            _isAuthenticated.value = hasCred
            tokenManager.invalidateCache()
        }
    }

    override suspend fun clearAuth() {
        authMutex.withLock {
            credentialStore.clearCredentials()
            tokenManager.invalidateCache()
            _sessionState.value = SessionState.Unauthenticated
            _isAuthenticated.value = false
        }
    }

    override suspend fun getValidTokens(): GeminiTokens {
        return tokenManager.getValidTokens()
    }

    override suspend fun forceRefreshToken(): GeminiTokens {
        return tokenManager.forceRefreshToken()
    }

    override suspend fun ensureValidTokens(forceRefresh: Boolean): GeminiTokens {
        return if (forceRefresh) forceRefreshToken() else getValidTokens()
    }
}
