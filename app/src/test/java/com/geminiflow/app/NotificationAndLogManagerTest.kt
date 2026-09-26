package com.geminiflow.app

import com.geminiflow.app.domain.model.common.AppNotification
import com.geminiflow.app.domain.model.common.NotificationType
import com.geminiflow.app.domain.model.log.ApiLogModel
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAndLogManagerTest {

    @Test
    fun testAppNotificationCreation() {
        val notification = AppNotification(
            message = "測試通知訊息",
            type = NotificationType.SUCCESS,
            durationMs = 3000L
        )

        assertNotNull(notification.id)
        assertEquals("測試通知訊息", notification.message)
        assertEquals(NotificationType.SUCCESS, notification.type)
        assertEquals(3000L, notification.durationMs)
        assertTrue(notification.timestamp > 0)
    }

    @Test
    fun testNotificationTypes() {
        val types = NotificationType.values()
        assertEquals(4, types.size)
        assertTrue(types.contains(NotificationType.SUCCESS))
        assertTrue(types.contains(NotificationType.ERROR))
        assertTrue(types.contains(NotificationType.INFO))
        assertTrue(types.contains(NotificationType.WARNING))
    }

    @Test
    fun testApiLogModelCreation() {
        val item = ApiLogModel(
            id = "test-123",
            file = File("/tmp/test.json"),
            title = "AiChat",
            displayTime = "2026/09/26 20:00:00",
            durationMs = 1500L,
            timestamp = 1790424000000L,
            rawJson = "{\"prompt\":\"hello\"}",
            source = "全部"
        )

        assertEquals("AiChat", item.title)
        assertEquals(1500L, item.durationMs)
        assertEquals("test-123", item.id)
        assertEquals(1790424000000L, item.timestampMillis)
    }

    @Test
    fun testJsonTreeParsing() {
        val rawJson = """
            {
                "timestamp": "2026-09-22T01:28:35.671303",
                "agentName": "AiChat",
                "durationMs": 11742,
                "request": {
                    "model": "gemini-1.5-flash",
                    "messages": [
                        {"role": "user", "content": "Hello"}
                    ]
                },
                "response": {
                    "text": "Hello there!",
                    "done": true
                }
            }
        """.trimIndent()

        val json = JSONObject(rawJson)
        assertEquals("AiChat", json.optString("agentName"))
        assertEquals(11742L, json.optLong("durationMs"))

        val req = json.getJSONObject("request")
        assertEquals("gemini-1.5-flash", req.getString("model"))
        assertEquals(1, req.getJSONArray("messages").length())

        val resp = json.getJSONObject("response")
        assertEquals("Hello there!", resp.getString("text"))
        assertEquals(true, resp.getBoolean("done"))
    }
}
