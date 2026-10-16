package com.geminiflow.app

import com.geminiflow.app.data.auth.token.HttpGeminiTokenExtractor
import com.geminiflow.app.domain.model.common.TokenExtractionException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class HttpGeminiTokenExtractorTest {

    @Test
    fun testParseHtmlSuccessWithStandardFormat() {
        val extractor = HttpGeminiTokenExtractor(OkHttpClient())
        val html = """
            <!doctype html>
            <html>
            <script>
            window.WIZ_global_data = {
                "SNlM0e":"AIzaSyMockToken12345",
                "FdrFJe":"1234567890123456789",
                "cfb2h":"boq_gemini-web-uiserver_20261016.12_p0"
            };
            </script>
            </html>
        """.trimIndent()

        val tokens = extractor.parseHtml(html)
        assertEquals("AIzaSyMockToken12345", tokens.snlm0e)
        assertEquals("1234567890123456789", tokens.sid)
        assertEquals("boq_gemini-web-uiserver_20261016.12_p0", tokens.serverBl)
    }

    @Test
    fun testParseHtmlSuccessWithEscapedAndWhitespaceFormat() {
        val extractor = HttpGeminiTokenExtractor(OkHttpClient())
        val html = """
            \"SNlM0e\": \"EscapedTokenABC_123\", \"FdrFJe\": \"987654\", \"cfb2h\": \"boq_gemini-web-uiserver_20261012.19_p1\"
        """.trimIndent()

        val tokens = extractor.parseHtml(html)
        assertEquals("EscapedTokenABC_123", tokens.snlm0e)
        assertEquals("987654", tokens.sid)
        assertEquals("boq_gemini-web-uiserver_20261012.19_p1", tokens.serverBl)
    }

    @Test
    fun testParseHtmlThrowsWhenSnlm0eMissing() {
        val extractor = HttpGeminiTokenExtractor(OkHttpClient())
        val html = "<html><body>Sign in to continue</body></html>"
        try {
            extractor.parseHtml(html)
            fail("Expected TokenExtractionException when SNlM0e is missing")
        } catch (e: TokenExtractionException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun testExtractTokensViaHttpSuccess() = runBlocking {
        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val html = """window.WIZ_global_data = {"SNlM0e":"net_token_abc","FdrFJe":"555"};"""
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body(html.toResponseBody("text/html".toMediaType()))
                .build()
        }).build()

        val extractor = HttpGeminiTokenExtractor(testClient)
        val tokens = extractor.extractTokens(mapOf("__Secure-1PSID" to "psid_val"))
        assertEquals("net_token_abc", tokens.snlm0e)
        assertEquals("555", tokens.sid)
    }

    @Test
    fun testExtractTokensViaHttpFailsOn500() = runBlocking {
        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(500)
                .message("Internal Server Error")
                .body("Internal error".toResponseBody("text/plain".toMediaType()))
                .build()
        }).build()

        val extractor = HttpGeminiTokenExtractor(testClient)
        try {
            extractor.extractTokens(mapOf("__Secure-1PSID" to "psid_val"))
            fail("Expected TokenExtractionException on HTTP 500")
        } catch (e: TokenExtractionException) {
            assertNotNull(e.message)
        }
    }
}
