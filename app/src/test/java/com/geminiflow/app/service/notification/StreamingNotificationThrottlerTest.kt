package com.geminiflow.app.service.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingNotificationThrottlerTest {

    @Test
    fun testThrottlingWithinInterval() {
        var currentTime = 1000L
        val throttler = StreamingNotificationThrottler(minIntervalMs = 400L) { currentTime }

        // 第 1 次發送：尚未記錄過，必須允許發送
        assertTrue(throttler.shouldEmit("req-1"))

        // 第 2 次發送（只經過 200ms）：必須被節流阻擋
        currentTime += 200L
        assertFalse(throttler.shouldEmit("req-1"))

        // 第 3 次發送（又經過 200ms，總共 400ms）：必須允許發送
        currentTime += 200L
        assertTrue(throttler.shouldEmit("req-1"))
    }

    @Test
    fun testDifferentRequestsAreIsolated() {
        var currentTime = 1000L
        val throttler = StreamingNotificationThrottler(minIntervalMs = 400L) { currentTime }

        assertTrue(throttler.shouldEmit("req-1"))
        // req-2 是獨立的請求，即使同一毫秒也必須允許發送
        assertTrue(throttler.shouldEmit("req-2"))
    }

    @Test
    fun testReleaseAllowsImmediateReEmission() {
        var currentTime = 1000L
        val throttler = StreamingNotificationThrottler(minIntervalMs = 400L) { currentTime }

        assertTrue(throttler.shouldEmit("req-1"))
        assertFalse(throttler.shouldEmit("req-1"))

        throttler.release("req-1")
        // release 後應被視為新的請求，允許發送
        assertTrue(throttler.shouldEmit("req-1"))
    }
}
