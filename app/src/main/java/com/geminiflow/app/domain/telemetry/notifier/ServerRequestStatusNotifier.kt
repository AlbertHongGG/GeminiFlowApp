package com.geminiflow.app.domain.telemetry.notifier

import com.geminiflow.app.domain.telemetry.model.ServerRequestLifecycleEvent

/**
 * 伺服器外部請求狀態通知領域介面
 * 抽象化狀態推播，使伺服器路由管線不直接依賴任何 Android 原生通知體系。
 */
interface ServerRequestStatusNotifier {
    /**
     * 當收到外部 HTTP 請求進入處理管線時觸發
     */
    fun notifyReceived(event: ServerRequestLifecycleEvent.Received)

    /**
     * 當正在進行串流傳輸 (SSE) 時觸發（內部應具備節流防抖）
     */
    fun notifyStreaming(event: ServerRequestLifecycleEvent.Streaming)

    /**
     * 當請求成功傳輸完畢時觸發
     */
    fun notifyCompleted(event: ServerRequestLifecycleEvent.Completed)

    /**
     * 當請求處理過程發生錯誤時觸發
     */
    fun notifyFailed(event: ServerRequestLifecycleEvent.Failed)
}
