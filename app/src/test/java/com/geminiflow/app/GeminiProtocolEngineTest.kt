package com.geminiflow.app

import com.geminiflow.app.data.api.protocol.GeminiPayloadBuilder
import com.geminiflow.app.data.api.protocol.GeminiProtocolEngine
import com.geminiflow.app.data.api.protocol.JspbHeaderBuilder
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.auth.SessionData
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ModelSpec
import com.geminiflow.app.domain.model.chat.ThinkingLevel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiProtocolEngineTest {

    private val testBl = "boq_gemini-web-uiserver_20261012.15_p0"
    private val tokens = GeminiTokens(snlm0e = "TEST_SNLM0E", sid = "123456789")

    @Test
    fun testJspbHeaderBuilder_flash() {
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
    fun testJspbHeaderBuilder_argon() {
        val spec = ModelSpec.resolve("gemini4Argon", "high")
        val builder = JspbHeaderBuilder(
            modelSpec = spec,
            sessionUuid = "SESSION-UUID-ARGON",
            reqUuid = "REQ-UUID-789",
            serverBl = testBl
        )
        val headers = builder.buildAllHeaders()
        val h525 = JSONArray(headers["x-goog-ext-525001261-jspb"])

        assertEquals("95a4c0abcba24364", h525.getString(4))
        assertEquals(8, h525.getInt(14))
        assertEquals(2, h525.getInt(15)) // high thinking = 2
    }

    @Test
    fun testTurn1_flash_thinking_low() {
        // Turn 1: gemini3.8Flash (think mode is low) "第一次測試"
        // New session (turn 0)
        // model_id = 1, model_hash = "56fdd199312815e2", thinking_level = 1
        // arr[96] = 0, arr[71] = null, arr[2] = null
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val req = ChatRequest(prompt = "第一次測試", model = "gemini-3.8-flash", thinking = ThinkingLevel.LOW)
        val proto = engine.buildRequest(req, tokens, sessionData = null)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals("56fdd199312815e2", h525.getString(4))
        assertEquals(1, h525.getInt(14))
        assertEquals(1, h525.getInt(15))

        assertEquals(99, inner.length())
        assertEquals("第一次測試", inner.getJSONArray(0).getString(0))
        assertTrue(inner.isNull(2))
        assertEquals(0, inner.getJSONArray(17).getJSONArray(0).getInt(0))
        assertTrue(inner.isNull(71))
        assertEquals(1, inner.getInt(79))
        assertEquals(1, inner.getInt(80))
        assertEquals(0, inner.getInt(96))
    }

    @Test
    fun testTurn2_pro_thinking_low() {
        // Turn 2: gemini3Pro (think mode is low) "第二次測試"
        // Continuing session (turn 1)
        // model_id = 3, model_hash = "e6fa609c3fa255c0", thinking_level = 1
        // arr[96] = 1, arr[71] = [conv_id, null*6, token]
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val session = SessionData(
            sessionId = "sess_1",
            conversationId = "c_fb6da9be329112e7",
            continuationToken = "AwAAAAAAAAAQANM7mBjXKZS5cPttExo",
            turnIndex = 1
        )
        val req = ChatRequest(prompt = "第二次測試", model = "gemini-3-pro", thinking = ThinkingLevel.LOW)
        val proto = engine.buildRequest(req, tokens, sessionData = session)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals("e6fa609c3fa255c0", h525.getString(4))
        assertEquals(3, h525.getInt(14))
        assertEquals(1, h525.getInt(15))

        assertEquals("第二次測試", inner.getJSONArray(0).getString(0))
        assertTrue(inner.isNull(2))
        assertEquals(1, inner.getJSONArray(17).getJSONArray(0).getInt(0))

        val ctx = inner.getJSONArray(71)
        assertEquals(8, ctx.length())
        assertEquals("c_fb6da9be329112e7", ctx.getString(0))
        for (i in 1..6) assertTrue(ctx.isNull(i))
        assertEquals("AwAAAAAAAAAQANM7mBjXKZS5cPttExo", ctx.getString(7))

        assertEquals(3, inner.getInt(79))
        assertEquals(1, inner.getInt(80))
        assertEquals(1, inner.getInt(96))
    }

    @Test
    fun testTurn3_argon_thinking_low() {
        // Turn 3: gemini4Argon (think mode is low) "第三次測試"
        // Continuing session (turn 2)
        // model_id = 8, model_hash = "95a4c0abcba24364", thinking_level = 1
        // arr[96] = 1, arr[71] = [conv_id, null*6, token]
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val session = SessionData(
            sessionId = "sess_1",
            conversationId = "c_fb6da9be329112e7",
            continuationToken = "AwAAAAAAAAAQANM7mBjXKZQ5aABuExo",
            turnIndex = 2
        )
        val req = ChatRequest(prompt = "第三次測試", model = "gemini4Argon", thinking = ThinkingLevel.LOW)
        val proto = engine.buildRequest(req, tokens, sessionData = session)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals("95a4c0abcba24364", h525.getString(4))
        assertEquals(8, h525.getInt(14))
        assertEquals(1, h525.getInt(15))

        assertEquals("第三次測試", inner.getJSONArray(0).getString(0))
        assertTrue(inner.isNull(2))
        assertEquals(2, inner.getJSONArray(17).getJSONArray(0).getInt(0))

        val ctx = inner.getJSONArray(71)
        assertEquals("c_fb6da9be329112e7", ctx.getString(0))
        assertEquals("AwAAAAAAAAAQANM7mBjXKZQ5aABuExo", ctx.getString(7))

        assertEquals(8, inner.getInt(79))
        assertEquals(1, inner.getInt(80))
        assertEquals(1, inner.getInt(96))
    }

    @Test
    fun testTurn4_argon_thinking_medium() {
        // Turn 4: gemini4Argon (think mode is medium) "第四次測試"
        // Continuing session (turn 3)
        // model_id = 8, model_hash = "95a4c0abcba24364", thinking_level = 4
        // arr[96] = 1, arr[71] = [conv_id, null*6, token]
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val session = SessionData(
            sessionId = "sess_1",
            conversationId = "c_fb6da9be329112e7",
            continuationToken = "AwAAAAAAAAAQANM7mBjXKZSp5ANuExo",
            turnIndex = 3
        )
        val req = ChatRequest(prompt = "第四次測試", model = "gemini4Argon", thinking = ThinkingLevel.MEDIUM)
        val proto = engine.buildRequest(req, tokens, sessionData = session)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals("95a4c0abcba24364", h525.getString(4))
        assertEquals(8, h525.getInt(14))
        assertEquals(4, h525.getInt(15)) // medium thinking = 4

        assertEquals("第四次測試", inner.getJSONArray(0).getString(0))
        assertTrue(inner.isNull(2))
        assertEquals(3, inner.getJSONArray(17).getJSONArray(0).getInt(0))

        val ctx = inner.getJSONArray(71)
        assertEquals("c_fb6da9be329112e7", ctx.getString(0))
        assertEquals("AwAAAAAAAAAQANM7mBjXKZSp5ANuExo", ctx.getString(7))

        assertEquals(8, inner.getInt(79))
        assertEquals(4, inner.getInt(80)) // medium thinking = 4
        assertEquals(1, inner.getInt(96))
    }

    @Test
    fun testTurn5_argon_thinking_high() {
        // Turn 5: gemini4Argon (think mode is high) "第五次測試"
        // Continuing session (turn 4)
        // model_id = 8, model_hash = "95a4c0abcba24364", thinking_level = 2
        // arr[96] = 1, arr[71] = [conv_id, null*6, token]
        val engine = GeminiProtocolEngine(serverBl = testBl)
        val session = SessionData(
            sessionId = "sess_1",
            conversationId = "c_fb6da9be329112e7",
            continuationToken = "AwAAAAAAAAAQANM7mBjXKZTJdQhuExo",
            turnIndex = 4
        )
        val req = ChatRequest(prompt = "第五次測試", model = "gemini4Argon", thinking = ThinkingLevel.HIGH)
        val proto = engine.buildRequest(req, tokens, sessionData = session)

        val h525 = JSONArray(proto.headers["x-goog-ext-525001261-jspb"])
        val fReq = JSONArray(proto.postData["f.req"])
        val inner = JSONArray(fReq.getString(1))

        assertEquals("95a4c0abcba24364", h525.getString(4))
        assertEquals(8, h525.getInt(14))
        assertEquals(2, h525.getInt(15)) // high thinking = 2

        assertEquals("第五次測試", inner.getJSONArray(0).getString(0))
        assertTrue(inner.isNull(2))
        assertEquals(4, inner.getJSONArray(17).getJSONArray(0).getInt(0))

        val ctx = inner.getJSONArray(71)
        assertEquals("c_fb6da9be329112e7", ctx.getString(0))
        assertEquals("AwAAAAAAAAAQANM7mBjXKZTJdQhuExo", ctx.getString(7))

        assertEquals(8, inner.getInt(79))
        assertEquals(2, inner.getInt(80)) // high thinking = 2
        assertEquals(1, inner.getInt(96))
    }
}
