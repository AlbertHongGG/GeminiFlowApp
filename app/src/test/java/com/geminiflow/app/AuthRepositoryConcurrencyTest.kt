package com.geminiflow.app

import com.geminiflow.app.data.auth.CookieManagerHelper
import com.geminiflow.app.data.repository.AuthRepositoryImpl
import com.geminiflow.app.data.repository.ChatRepositoryImpl
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AuthRepositoryConcurrencyTest {

    @Test
    fun testChatRepositoryTransparentSelfHealingOn401() = runBlocking {
        var callCount = 0
        var forceRefreshCount = 0

        val fakeAuthRepository = object : AuthRepository {
            override val sessionState: StateFlow<com.geminiflow.app.domain.model.auth.SessionState> =
                MutableStateFlow(com.geminiflow.app.domain.model.auth.SessionState.Authenticated())
            override val isAuthenticated: StateFlow<Boolean> = MutableStateFlow(true)
            private var currentTokens = GeminiTokens(snlm0e = "old_snlm0e", sid = "123")

            override suspend fun getValidTokens(): GeminiTokens = currentTokens

            override suspend fun forceRefreshToken(): GeminiTokens {
                forceRefreshCount++
                currentTokens = GeminiTokens(snlm0e = "new_snlm0e", sid = "456")
                return currentTokens
            }

            override suspend fun ensureValidTokens(forceRefresh: Boolean): GeminiTokens =
                if (forceRefresh) forceRefreshToken() else getValidTokens()

            override suspend fun getGoogleCookies(): Map<String, String> =
                mapOf("__Secure-1PSID" to "mock_psid")

            override suspend fun saveCookies(cookiesHeader: String) {}
            override suspend fun clearAuth() {}
        }

        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            callCount++
            if (callCount == 1) {
                // First request: Google returns 401 Unauthorized (token expired)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .code(401)
                    .message("Unauthorized")
                    .body("Token expired".toResponseBody("text/plain".toMediaType()))
                    .build()
            } else {
                // Second request (after transparent retry): Google returns 200 OK
                val sseBody = """[["wrb.fr",null,"[null,[\"c_12345\",null,null,null,null,null,null,\"AwAAAAAAAAAQANM7mBjXKZS5cPttExo\"],null,null,[null,null,null,null,[[\"test_id\",[\"Hello world!\"],null,null,null,null,null,null,null,null,[]]]]]"]]""" + "\n\n"
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .body(sseBody.toResponseBody("text/event-stream".toMediaType()))
                    .build()
            }
        }).build()

        val chatRepository = ChatRepositoryImpl(
            client = testClient,
            authRepository = fakeAuthRepository
        )

        val request = ChatRequest(prompt = "Hello transparent auth", model = "gemini-3.8-flash")
        val chunks = chatRepository.streamGenerate(request, sessionData = null).toList()

        // 驗證第一次 401 觸發自動換證，第二輪重試成功回傳資料
        assertTrue(chunks.isNotEmpty())
        val textChunk = chunks.firstOrNull { !it.text.isNullOrEmpty() }
        assertNotNull(textChunk)
        assertEquals("Hello world!", textChunk?.text)
        assertEquals(2, callCount)
        assertEquals(1, forceRefreshCount)
    }

    @Test
    fun testSingleFlightTokenRefreshUnderConcurrency() = runBlocking {
        val networkCallCount = AtomicInteger(0)

        // Mock OkHttpClient returning Gemini HTML page with SNlM0e token
        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            networkCallCount.incrementAndGet()
            Thread.sleep(50) // simulate network latency
            val html = """<html><script>var x = {"SNlM0e":"mock_snlm0e_concurrent_test", "FdrFJe":"987654321", "cfb2h":"boq_gemini-web-uiserver_test"};</script></html>"""
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body(html.toResponseBody("text/html".toMediaType()))
                .build()
        }).build()

        val mockCredentialStore = object : com.geminiflow.app.data.auth.CredentialStore {
            override fun getCookies(): Map<String, String> = mapOf("__Secure-1PSID" to "mock_valid_psid")
            override fun hasRequiredCredential(): Boolean = true
            override fun saveCookies(cookiesHeader: String) {}
            override fun clearCredentials() {}
        }

        val tokenExtractor = com.geminiflow.app.data.auth.token.HttpGeminiTokenExtractor(testClient)
        val tokenManager = com.geminiflow.app.data.auth.token.GeminiTokenManager(mockCredentialStore, tokenExtractor)
        val authRepository = AuthRepositoryImpl(mockCredentialStore, tokenManager)

        // 發起 10 個併發協程同時獲取 tokens
        val deferredList = (1..10).map {
            async {
                authRepository.getValidTokens()
            }
        }

        val results = deferredList.awaitAll()

        // 驗證所有 10 個協程皆拿到相同的 SNlM0e Token
        assertEquals(10, results.size)
        results.forEach { token ->
            assertEquals("mock_snlm0e_concurrent_test", token.snlm0e)
            assertEquals("987654321", token.sid)
        }

        // 驗證透過 Mutex 與雙檢鎖機制，底層網路請求僅被精確呼叫 1 次（Single-flight 防擊穿）
        assertEquals(1, networkCallCount.get())

        // 呼叫 forceRefreshToken() 驗證強制換證會觸發第二次網路請求
        val refreshed = authRepository.forceRefreshToken()
        assertEquals("mock_snlm0e_concurrent_test", refreshed.snlm0e)
        assertEquals(2, networkCallCount.get())
    }

    @Test
    fun testTokenExtractionFailureDoesNotDemoteIsAuthenticated() = runBlocking {
        // Mock OkHttpClient returning 500 or invalid HTML without SNlM0e
        val failingClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(500)
                .message("Internal Server Error")
                .body("<html><body>Server Error</body></html>".toResponseBody("text/html".toMediaType()))
                .build()
        }).build()

        val mockCredentialStore = object : com.geminiflow.app.data.auth.CredentialStore {
            private var hasCookie = true
            override fun getCookies(): Map<String, String> {
                return if (hasCookie) mapOf("__Secure-1PSID" to "mock_valid_psid") else emptyMap()
            }
            override fun hasRequiredCredential(): Boolean = hasCookie
            override fun saveCookies(cookiesHeader: String) {}
            override fun clearCredentials() {
                hasCookie = false
            }
        }

        val tokenExtractor = com.geminiflow.app.data.auth.token.HttpGeminiTokenExtractor(failingClient)
        val tokenManager = com.geminiflow.app.data.auth.token.GeminiTokenManager(mockCredentialStore, tokenExtractor)
        val authRepository = AuthRepositoryImpl(mockCredentialStore, tokenManager)

        // 初始狀態為已登入 (SessionState.Authenticated)
        assertTrue(authRepository.isAuthenticated.value)
        assertTrue(authRepository.sessionState.value is com.geminiflow.app.domain.model.auth.SessionState.Authenticated)

        // 當網路請求或 HTML 解析 SNlM0e 失敗時，應拋出 TokenExtractionException，但絕對不可篡改使用者登入狀態
        try {
            authRepository.getValidTokens()
            org.junit.Assert.fail("Expected TokenExtractionException but none was thrown")
        } catch (e: Exception) {
            assertTrue(e is com.geminiflow.app.domain.model.common.TokenExtractionException)
        }

        // 驗證核心不變量：使用者登入狀態依然為 true / Authenticated，系統絕不越權登出使用者
        assertTrue(authRepository.isAuthenticated.value)
        assertTrue(authRepository.sessionState.value is com.geminiflow.app.domain.model.auth.SessionState.Authenticated)

        // 只有明確調用 clearAuth() 時，狀態才轉為 false / Unauthenticated
        authRepository.clearAuth()
        org.junit.Assert.assertFalse(authRepository.isAuthenticated.value)
        assertTrue(authRepository.sessionState.value is com.geminiflow.app.domain.model.auth.SessionState.Unauthenticated)
    }
}
