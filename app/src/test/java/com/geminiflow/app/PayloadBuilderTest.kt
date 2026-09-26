package com.geminiflow.app

import com.geminiflow.app.data.api.payload.PayloadBuilderFactory
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.auth.GeminiTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadBuilderTest {

    @Test
    fun testProModelPayloadBuilder() {
        val request = ChatRequest(
            prompt = "Hello Gemini",
            model = "gemini-3-pro"
        )
        val tokens = GeminiTokens(
            snlm0e = "mock_snlm0e_token",
            sid = "123456789"
        )

        val builder = PayloadBuilderFactory.create(request, tokens)
        val params = builder.buildParams()

        assertEquals("zh-TW", params["hl"])
        assertEquals("123456789", params["f.sid"])
        assertNotNull(params["bl"])

        val payload = builder.buildPayload()
        assertEquals("mock_snlm0e_token", payload["at"])
        assertTrue(payload.containsKey("f.req"))

        val headers = builder.buildHeaders()
        assertTrue(headers.containsKey("x-goog-ext-525005358-jspb"))
    }
}
