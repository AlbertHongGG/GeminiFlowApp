package com.geminiflow.app

import com.geminiflow.app.domain.model.log.ApiLogRecord
import com.geminiflow.app.domain.model.log.ApiLogRequest
import com.geminiflow.app.domain.model.log.ApiLogResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiLogRecordTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Test
    fun serialization_omitsNullFieldsCompletely() {
        val record = ApiLogRecord(
            timestamp = "2026-09-27T19:50:00.000000",
            agentName = "AiChat",
            durationMs = 1200L,
            request = ApiLogRequest(
                model = "gemini-3.7-flash",
                prompt = "哈囉，請問今天天氣如何？",
                systemPrompt = null,
                sessionId = null,
                images = null
            ),
            response = ApiLogResponse(
                text = "今天天氣晴朗，適合外出散步！",
                images = null,
                error = null
            )
        )

        val jsonString = json.encodeToString(record)

        // 驗證存在必要欄位
        assertTrue(jsonString.contains("\"model\": \"gemini-3.7-flash\""))
        assertTrue(jsonString.contains("\"prompt\": \"哈囉，請問今天天氣如何？\""))
        assertTrue(jsonString.contains("\"text\": \"今天天氣晴朗，適合外出散步！\""))

        // 驗證未提供之欄位徹底不輸出（零幽靈欄位原則）
        assertFalse(jsonString.contains("system_prompt"))
        assertFalse(jsonString.contains("session_id"))
        assertFalse(jsonString.contains("\"images\""))
        assertFalse(jsonString.contains("error"))
        assertFalse(jsonString.contains("SSE Stream Complete"))
    }

    @Test
    fun serialization_includesProvidedOptionalFields() {
        val record = ApiLogRecord(
            timestamp = "2026-09-27T19:50:00.000000",
            agentName = "AiChat",
            durationMs = 2300L,
            request = ApiLogRequest(
                model = "gemini-3.7-flash",
                prompt = "請分析這張圖片",
                systemPrompt = "你是一位精通日本藥妝的專業助理。",
                sessionId = "session-12345",
                images = listOf("http://127.0.0.1:5000/images/test.jpg")
            ),
            response = ApiLogResponse(
                text = "這張圖片顯示的是大正百保能感冒藥。",
                images = listOf("http://127.0.0.1:5000/images/result.png"),
                error = null
            )
        )

        val jsonString = json.encodeToString(record)

        // 驗證有提供的可選欄位均有輸出
        assertTrue(jsonString.contains("\"system_prompt\": \"你是一位精通日本藥妝的專業助理。\""))
        assertTrue(jsonString.contains("\"session_id\": \"session-12345\""))
        assertTrue(jsonString.contains("\"images\""))
        assertTrue(jsonString.contains("http://127.0.0.1:5000/images/test.jpg"))
        assertTrue(jsonString.contains("http://127.0.0.1:5000/images/result.png"))

        // 驗證 error 為 null 時依然不輸出
        assertFalse(jsonString.contains("error"))
    }

    @Test
    fun roundTrip_serializesAndDeserializesAccurately() {
        val original = ApiLogRecord(
            timestamp = "2026-09-27T19:50:00.000000",
            agentName = "Playground (gemini-3-pro)",
            durationMs = 500L,
            request = ApiLogRequest(
                model = "gemini-3-pro",
                prompt = "簡單測試"
            ),
            response = ApiLogResponse(
                text = "測試回覆",
                error = null
            )
        )

        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<ApiLogRecord>(jsonString)

        assertEquals(original.agentName, deserialized.agentName)
        assertEquals(original.request.prompt, deserialized.request.prompt)
        assertEquals(original.response.text, deserialized.response.text)
        assertEquals(null, deserialized.request.systemPrompt)
        assertEquals(null, deserialized.response.images)
    }
}
