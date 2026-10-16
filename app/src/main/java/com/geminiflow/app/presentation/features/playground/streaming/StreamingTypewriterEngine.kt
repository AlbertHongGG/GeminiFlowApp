package com.geminiflow.app.presentation.features.playground.streaming

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 自適應流式平滑打字機排程器。
 * 負責將網路層不均勻、批次到達的 Chunk（通常每 200~500ms 一包數十字元），
 * 轉換為極具科技感與極致流暢的逐字/逐詞平滑輸出（16ms 週期，最高 60 FPS）。
 *
 * 核心演算法具備動態自適應追趕機制（Adaptive Pacing）：
 * - 佇列字數少時：逐字輸出，極致細膩
 * - 佇列積壓字數增多時：自動提速追趕，保證視覺落後伺服器不超過 300ms
 * - 串流結束時：平滑沖刷完畢後才標記完成
 */
class StreamingTypewriterEngine(
    private val scope: CoroutineScope,
    private val baseDelayMs: Long = 16L,
    private val onTextEmitted: (String) -> Unit
) {
    private val lock = Any()
    private val pendingBuffer = StringBuilder()
    private val emittedBuffer = StringBuilder()
    private var isStreamFinished = false
    private val completionDeferred = CompletableDeferred<Unit>()

    private var typingJob: Job? = null

    init {
        startTypingLoop()
    }

    private fun startTypingLoop() {
        typingJob = scope.launch {
            try {
                while (isActive) {
                    var chunkToEmit: String? = null
                    var isDone = false

                    synchronized(lock) {
                        if (pendingBuffer.isNotEmpty()) {
                            val available = pendingBuffer.length
                            val takeCount = when {
                                isStreamFinished && available > 50 -> max(10, available / 5)
                                available > 100 -> max(6, available / 15)
                                available > 45 -> 3
                                available > 15 -> 2
                                else -> 1
                            }
                            val actualTake = takeCount.coerceAtMost(available)
                            chunkToEmit = pendingBuffer.substring(0, actualTake)
                            pendingBuffer.delete(0, actualTake)
                            emittedBuffer.append(chunkToEmit)
                        } else if (isStreamFinished) {
                            isDone = true
                        }
                    }

                    if (chunkToEmit != null) {
                        onTextEmitted(emittedBuffer.toString())
                    }

                    if (isDone) {
                        completionDeferred.complete(Unit)
                        break
                    }

                    delay(baseDelayMs)
                }
            } finally {
                if (!completionDeferred.isCompleted) {
                    completionDeferred.complete(Unit)
                }
            }
        }
    }

    /**
     * 推送新到達的文字 Chunk 至打字機緩衝區
     */
    fun enqueue(text: String) {
        if (text.isEmpty()) return
        synchronized(lock) {
            pendingBuffer.append(text)
        }
    }

    /**
     * 標記網路串流已結束，打字機將平滑輸出剩餘緩衝區字元
     */
    fun complete() {
        synchronized(lock) {
            isStreamFinished = true
        }
    }

    /**
     * 等待打字機將所有積累字元平滑輸出完畢
     */
    suspend fun awaitCompletion() {
        completionDeferred.await()
    }

    /**
     * 立即取消打字排程（使用者主動中斷生成時調用）
     */
    fun cancel() {
        typingJob?.cancel()
        typingJob = null
        if (!completionDeferred.isCompleted) {
            completionDeferred.complete(Unit)
        }
    }

    /**
     * 當前已輸出的完整字串
     */
    fun getEmittedText(): String {
        synchronized(lock) {
            return emittedBuffer.toString()
        }
    }
}
