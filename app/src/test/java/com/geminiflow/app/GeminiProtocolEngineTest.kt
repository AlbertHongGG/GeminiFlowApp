package com.geminiflow.app

import com.geminiflow.app.data.api.protocol.GeminiPayloadBuilder
import com.geminiflow.app.data.api.protocol.GeminiProtocolEngine
import com.geminiflow.app.data.api.protocol.JspbHeaderBuilder
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ModelSpec
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiProtocolEngineTest {

    private val testBl = "boq_gemini-web-uiserver_20261012.15_p0"

    @Test
    fun testJspbHeaderBuilder_structure() {
        val spec = ModelSpec.resolve("gemini-3.8-flash", "medium")
        val builder = JspbHeaderBuilder(
            modelSpec = spec,
            sessionUuid = "SESSION-UUID-123",
            reqUuid = "REQ-UUID-456",
            serverBl = testBl
        )
        val headers = builder.buildAllHeaders()
        val h525 = JSONArray(headers["x-goog-ext-525001261-jspb"])
        val h535 = JSONArray(headers["x-goog-ext-525005358-jspb"])

        assertEquals(20, h525.length())
        assertEquals(1, h525.getInt(0))
        assertEquals("56fdd199312815e2", h525.getString(4))
        assertEquals(0, h525.getInt(7))
        assertEquals(14, h525.getJSONArray(8).length())
        assertEquals(2, h525.getInt(11))
        assertEquals(1, h525.getInt(14))
        assertEquals(4, h525.getInt(15)) // medium thinking = 4
        assertEquals("SESSION-UUID-123", h525.getString(16))
        assertEquals(testBl, h525.getJSONArray(17).getString(0))

        assertEquals("REQ-UUID-456", h535.getString(0))
        assertEquals(1, h535.getInt(1))
    }

    @Test
    fun testGeminiPayloadBuilder_structure() {
        val spec = ModelSpec.resolve("gemini-3-pro", "high")
        val builder = GeminiPayloadBuilder(
            prompt = "Hello Pro",
            systemPrompt = null,
            language = "zh-TW",
            modelSpec = spec,
            reqUuid = "REQ-UUID-PRO",
            turnIndex = 3
        )
        val arr = builder.buildInnerArray()

        assertEquals(99, arr.length())
        assertEquals("Hello Pro", arr.getJSONArray(0).getString(0))
        assertEquals("zh-TW", arr.getJSONArray(1).getString(0))

        // Turn index = 3
        assertEquals(3, arr.getJSONArray(17).getJSONArray(0).getInt(0))

        // Capabilities: [4, 16, 19]
        val caps = arr.getJSONArray(30)
        assertEquals(3, caps.length())
        assertEquals(4, caps.getInt(0))
        assertEquals(16, caps.getInt(1))
        assertEquals(19, caps.getInt(2))

        assertEquals("REQ-UUID-PRO", arr.getString(59))
        assertEquals(0, arr.getJSONArray(61).length())

        // Model ID and Thinking Level
        assertEquals(3, arr.getInt(79)) // Pro = 3
        assertEquals(2, arr.getInt(80)) // High = 2

        // End flags
        assertEquals(1, arr.getInt(91))
        assertEquals(1, arr.getInt(96))
        assertEquals(1, arr.getInt(98))
    }

    @Test
    fun testProtocolEngine_flashLow() {
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val req = ChatRequest(prompt = "第1次測試", model = "gemini-3.8-flash", thinking = ThinkingLevel.LOW)
        val tokens = GeminiTokens(snlm0e = "TEST_SNLM0E", sid = "123456789")

        val proto = engine.buildRequest(req, tokens, turnIndex = 0)

        assertEquals(testBl, proto.params["bl"])
        assertEquals("zh-TW", proto.params["hl"])
        assertEquals("123456789", proto.params["f.sid"])
        assertEquals("TEST_SNLM0E", proto.postData["at"])

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        assertEquals(1, h525.getInt(14))
        assertEquals(1, h525.getInt(15)) // Low = 1

        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))
        assertEquals(99, inner.length())
        assertEquals(1, inner.getInt(79))
        assertEquals(1, inner.getInt(80)) // Low = 1
    }

    @Test
    fun testProtocolEngine_proHighWithConversationContext() {
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val req = ChatRequest(prompt = "延續對話", model = "gemini-3-pro", thinking = ThinkingLevel.HIGH)
        val tokens = GeminiTokens(snlm0e = "TEST_SNLM0E", sid = "123456789")
        val convIds = listOf("c_test_conv", "r_test_resp", "rc_test_choice")

        val proto = engine.buildRequest(req, tokens, conversationIds = convIds, turnIndex = 2)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        assertEquals(3, h525.getInt(14)) // Pro = 3
        assertEquals(2, h525.getInt(15)) // High = 2

        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals(3, inner.getInt(79))
        assertEquals(2, inner.getInt(80))
        assertEquals(2, inner.getJSONArray(17).getJSONArray(0).getInt(0))

        val convBlock = inner.getJSONArray(2)
        assertEquals("c_test_conv", convBlock.getString(0))
        assertEquals("r_test_resp", convBlock.getString(1))
        assertEquals("rc_test_choice", convBlock.getString(2))
    }
}
