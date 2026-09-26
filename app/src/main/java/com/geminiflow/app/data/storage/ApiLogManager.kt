package com.geminiflow.app.data.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * API 請求日誌檔案對象模型。
 */
data class ApiLogModel(
    val id: String,
    val file: File,
    val title: String,
    val displayTime: String,
    val durationMs: Long,
    val timestamp: Long,
    val rawJson: String,
    val source: String = "全部"
)

/**
 * API 請求日誌管理器，負責請求與回應日誌的檔案持久化存儲、讀取與清理。
 */
class ApiLogManager(private val context: Context) {

    companion object {
        private const val TAG = "ApiLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_api_logging_enabled"
        private const val PREFS_NAME = "gemini_flow_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val logDir = File(context.filesDir, "logs/agent").apply {
        if (!exists()) mkdirs()
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _logsFlow = MutableStateFlow<List<ApiLogModel>>(emptyList())
    val logsFlow: StateFlow<List<ApiLogModel>> = _logsFlow.asStateFlow()

    init {
        reload()
    }

    fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_LOGGING_ENABLED, true)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_LOGGING_ENABLED, enabled).apply()
    }

    /**
     * 寫入 API 交互日誌至檔案（若已開啟日誌記錄）。
     */
    fun logInteraction(agentName: String, durationMs: Long, request: Any?, response: Any?) {
        if (!isLoggingEnabled()) return

        scope.launch {
            try {
                val now = Date()
                val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
                val hhmmss = SimpleDateFormat("HHmmss", Locale.US).format(now)
                val random = Random.nextInt(100000, 999999)
                val safeAgentName = agentName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val fileName = "${yyyyMMdd}_${hhmmss}_${safeAgentName}_${random}.json"
                val file = File(logDir, fileName)

                val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).format(now)
                val rootJson = JSONObject().apply {
                    put("timestamp", isoTimestamp)
                    put("agentName", agentName)
                    put("durationMs", durationMs)

                    when (request) {
                        is JSONObject -> put("request", request)
                        is JSONArray -> put("request", request)
                        is String -> {
                            val parsed = parseJsonQuietly(request)
                            if (parsed != null) put("request", parsed) else put("request", request)
                        }
                        null -> put("request", JSONObject.NULL)
                        else -> put("request", request.toString())
                    }

                    when (response) {
                        is JSONObject -> put("response", response)
                        is JSONArray -> put("response", response)
                        is String -> {
                            val parsed = parseJsonQuietly(response)
                            if (parsed != null) put("response", parsed) else put("response", response)
                        }
                        null -> put("response", JSONObject.NULL)
                        else -> put("response", response.toString())
                    }
                }

                file.writeText(rootJson.toString(2))
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write API log: ${e.message}", e)
            }
        }
    }

    private fun parseJsonQuietly(str: String): Any? {
        val trimmed = str.trim()
        return try {
            if (trimmed.startsWith("{")) JSONObject(trimmed)
            else if (trimmed.startsWith("[")) JSONArray(trimmed)
            else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 重新載入所有日誌檔案，依最新時間排序。
     */
    fun reload() {
        scope.launch {
            val entries = loadAllEntries()
            _logsFlow.value = entries
        }
    }

    private fun loadAllEntries(): List<ApiLogModel> {
        val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                val fileName = file.name
                val parts = fileName.split("_")
                var title = if (parts.size >= 4) parts[2] else fileName.removeSuffix(".json")
                var dtMillis = file.lastModified()
                var displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dtMillis))
                var durationMs = 0L

                if (parts.size >= 2) {
                    val dateStr = parts[0]
                    val timeStr = parts[1]
                    if (dateStr.length == 8 && timeStr.length >= 6) {
                        val parsed = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse("${dateStr}_${timeStr.take(6)}")
                        if (parsed != null) {
                            dtMillis = parsed.time
                            displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(parsed)
                        }
                    }
                }

                val content = file.readText()
                val json = JSONObject(content)
                if (json.has("agentName")) {
                    title = json.optString("agentName", title)
                }
                durationMs = json.optLong("durationMs", 0L)

                ApiLogModel(
                    id = file.name,
                    file = file,
                    title = title,
                    displayTime = displayTime,
                    durationMs = durationMs,
                    timestamp = dtMillis,
                    rawJson = content,
                    source = "全部"
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.file.name }
    }

    /**
     * 刪除特定日誌檔案。
     */
    suspend fun clearLog(file: File) = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "clearLog: ${file.name}, deleted=$deleted")
            }
            // 立即過濾已刪除檔案，確保 UI 零延遲即刻響應
            _logsFlow.value = _logsFlow.value.filter {
                it.file.absolutePath != file.absolutePath && it.id != file.name
            }
            // 重新讀取磁碟校驗
            val entries = loadAllEntries()
            _logsFlow.value = entries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log file: ${e.message}", e)
        }
    }

    /**
     * 清空所有日誌檔案。
     */
    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        try {
            if (logDir.exists()) {
                logDir.listFiles()?.forEach { f ->
                    try {
                        if (f.isDirectory) f.deleteRecursively() else f.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete ${f.name}: ${e.message}")
                    }
                }
            }
            // 徹底清除狀態，確保 UI 即刻響應空列表
            _logsFlow.value = emptyList()
            Log.d(TAG, "clearAllLogs: all api logs cleared cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all logs: ${e.message}", e)
            _logsFlow.value = emptyList()
        }
    }
}
