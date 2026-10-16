package com.geminiflow.app.domain.telemetry

import com.geminiflow.app.domain.telemetry.classifier.ServerRequestErrorClassifier
import com.geminiflow.app.domain.telemetry.model.ServerRequestErrorAttribution
import com.geminiflow.app.domain.telemetry.model.ServerRequestLifecycleEvent
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerRequestLifecycleTest {

    private class FakeServerRequestStatusNotifier(
        var isEnabled: Boolean = true
    ) : ServerRequestStatusNotifier {
        val events = mutableListOf<ServerRequestLifecycleEvent>()

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
}
