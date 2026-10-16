package com.geminiflow.app.domain.telemetry

import com.geminiflow.app.domain.telemetry.classifier.ServerRequestErrorClassifier
import com.geminiflow.app.domain.telemetry.model.ServerRequestErrorAttribution
import com.geminiflow.app.domain.telemetry.model.ServerRequestLifecycleEvent
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier
import com.geminiflow.app.domain.telemetry.session.RequestTelemetrySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerRequestLifecycleTest {

    private class FakeServerRequestStatusNotifier(
        var isEnabled: Boolean = true
    ) : ServerRequestStatusNotifier {
        val events = mutableListOf<ServerRequestLifecycleEvent>()
        var clearCount = 0
        var lastClearedRequestId: String? = null

        override fun notifyReceived(event: ServerRequestLifecycleEvent.Received) {
            if (isEnabled) events.add(event)
        }

        override fun notifyStreaming(event: ServerRequestLifecycleEvent.Streaming) {
            if (isEnabled) events.add(event)
        }

        override fun notifyCompleted(event: ServerRequestLifecycleEvent.Completed) {
            if (isEnabled) events.add(event)
        }

        override fun notifyFailed(event: ServerRequestLifecycleEvent.Failed) {
            if (isEnabled) events.add(event)
        }

        override fun clear(requestId: String?) {
            clearCount++
            lastClearedRequestId = requestId
        }
    }

    @Test
    fun testNormalLifecycleFlow() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        val requestId = "req-123"

        // 1. 收到請求
        notifier.notifyReceived(
            ServerRequestLifecycleEvent.Received(
                requestId = requestId,
                endpoint = "/stream",
                model = "gemini-2.5-flash",
                promptSnippet = "Hello world",
                clientIp = "127.0.0.1"
            )
        )
        assertEquals(1, notifier.events.size)
        assertTrue(notifier.events[0] is ServerRequestLifecycleEvent.Received)

        // 2. 串流生成中
        notifier.notifyStreaming(
            ServerRequestLifecycleEvent.Streaming(
                requestId = requestId,
                endpoint = "/stream",
                charCount = 20,
                chunkIndex = 1,
                elapsedMs = 300L
            )
        )
        assertEquals(2, notifier.events.size)
        assertTrue(notifier.events[1] is ServerRequestLifecycleEvent.Streaming)

        // 3. 傳輸完畢
        notifier.notifyCompleted(
            ServerRequestLifecycleEvent.Completed(
                requestId = requestId,
                endpoint = "/stream",
                totalChars = 50,
                durationMs = 800L,
                statusCode = 200
            )
        )
        assertEquals(3, notifier.events.size)
        val completed = notifier.events[2] as ServerRequestLifecycleEvent.Completed
        assertEquals(200, completed.statusCode)
        assertEquals(50, completed.totalChars)
    }

    @Test
    fun testClientErrorLifecycleFlow() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        val requestId = "req-client-err"

        val errorAttribution = ServerRequestErrorClassifier.classify(
            throwable = null,
            manualStatusCode = 400,
            fallbackMessage = "缺少模型參數 (model)"
        )

        notifier.notifyFailed(
            ServerRequestLifecycleEvent.Failed(
                requestId = requestId,
                endpoint = "/stream",
                errorAttribution = errorAttribution,
                durationMs = 120L
            )
        )

        assertEquals(1, notifier.events.size)
        val failed = notifier.events[0] as ServerRequestLifecycleEvent.Failed
        assertTrue(failed.errorAttribution.isClientFault)
        assertEquals(400, failed.errorAttribution.statusCode)
        assertTrue(failed.errorAttribution is ServerRequestErrorAttribution.ClientFault)
    }

    @Test
    fun testServerErrorLifecycleFlow() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        val requestId = "req-server-err"

        val errorAttribution = ServerRequestErrorClassifier.classify(
            throwable = RuntimeException("Google API 500 Internal Error"),
            manualStatusCode = 500
        )

        notifier.notifyFailed(
            ServerRequestLifecycleEvent.Failed(
                requestId = requestId,
                endpoint = "/chat",
                errorAttribution = errorAttribution,
                durationMs = 500L
            )
        )

        assertEquals(1, notifier.events.size)
        val failed = notifier.events[0] as ServerRequestLifecycleEvent.Failed
        assertFalse(failed.errorAttribution.isClientFault)
        assertEquals(500, failed.errorAttribution.statusCode)
        assertTrue(failed.errorAttribution is ServerRequestErrorAttribution.ServerFault)
    }

    @Test
    fun testBypassWhenDisabled() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = false)

        notifier.notifyReceived(
            ServerRequestLifecycleEvent.Received(
                requestId = "req-disabled",
                endpoint = "/stream",
                model = "gemini-2.5-flash",
                promptSnippet = "Hi",
                clientIp = "127.0.0.1"
            )
        )

        notifier.notifyCompleted(
            ServerRequestLifecycleEvent.Completed(
                requestId = "req-disabled",
                endpoint = "/stream",
                totalChars = 10,
                durationMs = 200L
            )
        )

        // 當功能開關關閉時，不應記錄任何事件
        assertTrue(notifier.events.isEmpty())
    }

    @Test
    fun testRequestTelemetrySessionEnsuresTerminalStateOnFailure() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        val session = RequestTelemetrySession(
            requestId = "req-session-test",
            endpoint = "/stream",
            clientIp = "127.0.0.1",
            notifier = notifier
        )

        session.markReceived(model = "gemini-2.5-flash", promptSnippet = "test")
        assertEquals(1, notifier.events.size)

        // 未主動呼叫 markCompleted / markFailed，模擬協程中斷或拋錯
        session.ensureTerminalState("異常退出測試")

        assertTrue(session.isCompletedOrFailed)
        assertEquals(2, notifier.events.size)
        val failed = notifier.events[1] as ServerRequestLifecycleEvent.Failed
        assertEquals(500, failed.errorAttribution.statusCode)

        // 再次呼叫 ensureTerminalState 或 markCompleted 應被 CAS 攔截，不可重複發送終態
        session.ensureTerminalState("再次退出")
        assertEquals(2, notifier.events.size)
        assertFalse(session.markCompleted(200, 100))
        assertEquals(2, notifier.events.size)
    }

    @Test
    fun testRequestTelemetrySessionAtomicTerminalState() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        val session = RequestTelemetrySession(
            requestId = "req-session-atomic",
            endpoint = "/chat",
            clientIp = "127.0.0.1",
            notifier = notifier
        )

        assertTrue(session.markCompleted(200, 50))
        // 第二次調用 markCompleted 應返回 false
        assertFalse(session.markCompleted(200, 50))
        // 調用 markFailed 應返回 null
        val attribution = session.markFailed(manualStatusCode = 500)
        assertEquals(null, attribution)

        assertEquals(1, notifier.events.size)
        assertTrue(notifier.events[0] is ServerRequestLifecycleEvent.Completed)
    }

    @Test
    fun testClearNotification() {
        val notifier = FakeServerRequestStatusNotifier(isEnabled = true)
        notifier.clear("req-999")
        assertEquals(1, notifier.clearCount)
        assertEquals("req-999", notifier.lastClearedRequestId)

        notifier.clear(null)
        assertEquals(2, notifier.clearCount)
        assertEquals(null, notifier.lastClearedRequestId)
    }
}
