package com.geminiflow.app

import com.geminiflow.app.data.auth.CredentialStore
import com.geminiflow.app.data.auth.token.GeminiTokenManager
import com.geminiflow.app.data.auth.token.TokenExtractor
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.common.AuthenticationRequiredException
import com.geminiflow.app.domain.model.common.TokenExtractionException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GeminiTokenManagerTest {

    @Test
    fun testSingleFlightConcurrencyUnder20Coroutines() = runBlocking {
        val extractCallCount = AtomicInteger(0)

        val fakeCredentialStore = object : CredentialStore {
            override fun getCookies(): Map<String, String> = mapOf("__Secure-1PSID" to "valid_psid")
            override fun hasRequiredCredential(): Boolean = true
            override fun saveCookies(cookiesHeader: String) {}
            override fun clearCredentials() {}
        }

        val fakeTokenExtractor = object : TokenExtractor {
            override suspend fun extractTokens(cookies: Map<String, String>): GeminiTokens {
                extractCallCount.incrementAndGet()
                delay(50) // 模擬網路延遲
                return GeminiTokens(snlm0e = "mock_snlm0e", sid = "123", serverBl = "bl_xyz")
            }
        }

        val tokenManager = GeminiTokenManager(fakeCredentialStore, fakeTokenExtractor)

        // 20 個併發協程同時搶奪權杖
        val deferreds = (1..20).map {
            async {
                tokenManager.getValidTokens()
            }
        }

        val results = deferreds.awaitAll()

        // 驗證 20 個協程均順利取得相同的權杖實體
        assertEquals(20, results.size)
        results.forEach { token ->
            assertEquals("mock_snlm0e", token.snlm0e)
            assertEquals("123", token.sid)
        }

        // 核心不變量：底層 extractTokens 精確只被呼叫 1 次！
        assertEquals(1, extractCallCount.get())

        // 快取命中：第 21 次調用直接走 Fast path，不增加網路呼叫
        val cached = tokenManager.getValidTokens()
        assertEquals("mock_snlm0e", cached.snlm0e)
        assertEquals(1, extractCallCount.get())

        // 強制重整：呼叫 forceRefreshToken() 會觸發第 2 次網路抽取
        val refreshed = tokenManager.forceRefreshToken()
        assertEquals("mock_snlm0e", refreshed.snlm0e)
        assertEquals(2, extractCallCount.get())
    }

    @Test
    fun testMissingCredentialThrowsAuthenticationRequiredException() = runBlocking {
        val emptyCredentialStore = object : CredentialStore {
            override fun getCookies(): Map<String, String> = emptyMap()
            override fun hasRequiredCredential(): Boolean = false
            override fun saveCookies(cookiesHeader: String) {}
            override fun clearCredentials() {}
        }

        val fakeTokenExtractor = object : TokenExtractor {
            override suspend fun extractTokens(cookies: Map<String, String>): GeminiTokens {
                fail("TokenExtractor should not be invoked when credential is missing")
                throw RuntimeException("unreachable")
            }
        }

        val tokenManager = GeminiTokenManager(emptyCredentialStore, fakeTokenExtractor)

        try {
            tokenManager.getValidTokens()
            fail("Expected AuthenticationRequiredException")
        } catch (e: Exception) {
            assertTrue(e is AuthenticationRequiredException)
        }
    }

    @Test
    fun testExtractorFailurePropagatesExceptionWithoutSettingState() = runBlocking {
        val fakeCredentialStore = object : CredentialStore {
            override fun getCookies(): Map<String, String> = mapOf("__Secure-1PSID" to "valid_psid")
            override fun hasRequiredCredential(): Boolean = true
            override fun saveCookies(cookiesHeader: String) {}
            override fun clearCredentials() {}
        }

        val failingExtractor = object : TokenExtractor {
            override suspend fun extractTokens(cookies: Map<String, String>): GeminiTokens {
                throw TokenExtractionException("HTTP 500 error")
            }
        }

        val tokenManager = GeminiTokenManager(fakeCredentialStore, failingExtractor)

        try {
            tokenManager.getValidTokens()
            fail("Expected TokenExtractionException")
        } catch (e: Exception) {
            assertTrue(e is TokenExtractionException)
            assertEquals("HTTP 500 error", e.message)
        }
    }
}
