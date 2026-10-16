package com.geminiflow.app.domain.telemetry.session

import com.geminiflow.app.domain.telemetry.classifier.ServerRequestErrorClassifier
import com.geminiflow.app.domain.telemetry.model.ServerRequestErrorAttribution
import com.geminiflow.app.domain.telemetry.model.ServerRequestLifecycleEvent
import com.geminiflow.app.domain.telemetry.notifier.ServerRequestStatusNotifier
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 伺服器請求遙測會話（Request Telemetry Session）
 * 專責生命週期守護，確保：
 * 1. 終態保證（Terminal State Guarantee）：每個請求最終必且僅觸發一次終態（Completed 或 Failed），杜絕殭屍懸掛。
 * 2. 執行續安全：利用 CAS（AtomicBoolean）防止並發重入與重複通知。
 * 3. 責任歸因：統一透過 ServerRequestErrorClassifier 將異常或狀態碼歸因給責任方。
 */
class RequestTelemetrySession(
    val requestId: String,
    val endpoint: String,
    val clientIp: String,
    private val notifier: ServerRequestStatusNotifier,
    val startTime: Long = System.currentTimeMillis()
) {
    private val isTerminated = AtomicBoolean(false)

    /**
     * 發送請求接收事件
     */
    fun markReceived(model: String?, promptSnippet: String?) {
        notifier.notifyReceived(
            ServerRequestLifecycleEvent.Received(
                requestId = requestId,
                endpoint = endpoint,
                model = model,
                promptSnippet = promptSnippet,
                clientIp = clientIp
            )
        )
    }

    /**
     * 發送串流處理中進度事件
     */
    fun notifyStreaming(charCount: Int, chunkIndex: Int) {
        if (isTerminated.get()) return
        notifier.notifyStreaming(
            ServerRequestLifecycleEvent.Streaming(
                requestId = requestId,
                endpoint = endpoint,
                charCount = charCount,
                chunkIndex = chunkIndex,
                elapsedMs = System.currentTimeMillis() - startTime
            )
        )
    }

    /**
     * 終態：請求成功傳輸完畢
     * @return true 若成功轉換為終態；false 若已處於終態
     */
    fun markCompleted(statusCode: Int = 200, totalChars: Int): Boolean {
        if (isTerminated.compareAndSet(false, true)) {
            val durationMs = System.currentTimeMillis() - startTime
            notifier.notifyCompleted(
                ServerRequestLifecycleEvent.Completed(
                    requestId = requestId,
                    endpoint = endpoint,
                    totalChars = totalChars,
                    durationMs = durationMs,
                    statusCode = statusCode
                )
            )
            return true
        }
        return false
    }

    /**
     * 終態：請求處理失敗或異常中斷
     * @return 診斷歸因結果，若已處於終態則返回 null
     */
    fun markFailed(
        throwable: Throwable? = null,
        manualStatusCode: Int? = null,
        fallbackMessage: String? = null
    ): ServerRequestErrorAttribution? {
        if (isTerminated.compareAndSet(false, true)) {
            val durationMs = System.currentTimeMillis() - startTime
            val attribution = ServerRequestErrorClassifier.classify(
                throwable = throwable,
                manualStatusCode = manualStatusCode,
                fallbackMessage = fallbackMessage
            )
            notifier.notifyFailed(
                ServerRequestLifecycleEvent.Failed(
                    requestId = requestId,
                    endpoint = endpoint,
                    errorAttribution = attribution,
                    durationMs = durationMs
                )
            )
            return attribution
        }
        return null
    }

    /**
     * 兜底防護：若區塊退出時未轉換為終態，自動轉換為 Failed 終態，防止殭屍卡死
     */
    fun ensureTerminalState(fallbackReason: String = "請求在未發送完成訊號前結束") {
        if (!isTerminated.get()) {
            markFailed(
                manualStatusCode = 500,
                fallbackMessage = fallbackReason
            )
        }
    }

    val isCompletedOrFailed: Boolean
        get() = isTerminated.get()
}
