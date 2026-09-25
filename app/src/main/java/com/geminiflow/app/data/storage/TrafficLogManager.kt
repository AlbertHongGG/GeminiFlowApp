package com.geminiflow.app.data.storage

import com.geminiflow.app.domain.model.TrafficLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Collections
import java.util.LinkedList

/**
 * 記憶體即時環狀緩衝區，維護近期的 HTTP 網路請求記錄。
 */
class TrafficLogManager(private val maxEntries: Int = 100) {

    private val _logs = MutableStateFlow<List<TrafficLog>>(emptyList())
    val logs: StateFlow<List<TrafficLog>> = _logs.asStateFlow()

    private val buffer = Collections.synchronizedList(LinkedList<TrafficLog>())

    fun record(log: TrafficLog) {
        synchronized(buffer) {
            buffer.add(0, log)
            if (buffer.size > maxEntries) {
                buffer.removeAt(buffer.size - 1)
            }
            _logs.update { ArrayList(buffer) }
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _logs.value = emptyList()
        }
    }
}
