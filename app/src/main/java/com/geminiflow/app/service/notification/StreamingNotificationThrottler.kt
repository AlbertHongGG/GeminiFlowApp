package com.geminiflow.app.service.notification

import java.util.concurrent.ConcurrentHashMap

/**
 * 串流通知節流控制器
 * 針對高頻 SSE 串流 chunk 事件進行時間戳防抖，避免頻繁調用 NotificationManager 導致系統卡頓或觸發頻率限制。
 */
class StreamingNotificationThrottler(
    private val minIntervalMs: Long = 400L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()

    /**
     * 判定指定 requestId 是否已達到可更新通知的間隔時間
     */
    fun shouldEmit(requestId: String): Boolean {
        val now = clock()
        val last = lastUpdateTimes[requestId] ?: 0L
        if (now - last >= minIntervalMs) {
            lastUpdateTimes[requestId] = now
            return true
        }
        return false
    }

    /**
     * 請求結束時釋放暫存的時間戳
     */
    fun release(requestId: String) {
        lastUpdateTimes.remove(requestId)
    }

    /**
     * 清空所有追蹤狀態
     */
    fun clear() {
        lastUpdateTimes.clear()
    }
}
