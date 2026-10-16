package com.geminiflow.app.data.auth.token

import com.geminiflow.app.data.api.GeminiConfig
import com.geminiflow.app.data.auth.CredentialStore
import com.geminiflow.app.domain.auth.TokenManager
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [TokenManager] 核心實作。
 * 採用 Double-Checked Locking 與協程 Mutex 實現 Single-Flight 單飛換證，
 * 保證即使 100 個協程同時呼叫，向 Google 發起的 HTTP 換證請求精準只有 1 次。
 */
class GeminiTokenManager(
    private val credentialStore: CredentialStore,
    private val tokenExtractor: TokenExtractor
) : TokenManager {

    private val mutex = Mutex()
    private var cachedTokens: GeminiTokens? = null

    override suspend fun getValidTokens(): GeminiTokens = withContext(Dispatchers.IO) {
        // Fast path: Double-Checked Locking Check 1 (lock-free)
        cachedTokens?.let { return@withContext it }

        // Slow path: Acquire mutex for single-flight resolution
        mutex.withLock {
            // Check 2 inside mutex
            cachedTokens?.let { return@withContext it }
            fetchAndCache()
        }
    }

    override suspend fun forceRefreshToken(): GeminiTokens = withContext(Dispatchers.IO) {
        mutex.withLock {
            fetchAndCache()
        }
    }

    override fun invalidateCache() {
        cachedTokens = null
    }

    private suspend fun fetchAndCache(): GeminiTokens {
        val cookies = credentialStore.getCookies()
        val pSid = cookies[GeminiConfig.REQUIRED_COOKIE_NAME]
        if (pSid.isNullOrBlank()) {
            cachedTokens = null
            throw AuthenticationRequiredException("缺少必要的 Google 驗證憑證 (${GeminiConfig.REQUIRED_COOKIE_NAME})，請在 App 內登入 Google 帳號。")
        }

        val tokens = tokenExtractor.extractTokens(cookies)
        cachedTokens = tokens
        return tokens
    }
}
