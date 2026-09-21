package com.geminiflow.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.geminiflow.app.domain.model.SessionData
import com.geminiflow.app.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SessionPreferencesRepository(
    context: Context
) : SessionRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemini_flow_sessions", Context.MODE_PRIVATE)

    override suspend fun getSession(sessionId: String): SessionData? = withContext(Dispatchers.IO) {
        val jsonStr = prefs.getString(sessionId, null) ?: return@withContext null
        try {
            val jsonArray = JSONArray(jsonStr)
            val ids = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                ids.add(jsonArray.getString(i))
            }
            if (ids.size >= 2) {
                return@withContext SessionData(sessionId = sessionId, conversationIds = ids)
            }
        } catch (_: Exception) {
        }
        null
    }

    override suspend fun saveSession(sessionData: SessionData): Unit = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray(sessionData.conversationIds)
        prefs.edit().putString(sessionData.sessionId, jsonArray.toString()).apply()
    }

    override suspend fun clearSession(sessionId: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove(sessionId).apply()
    }
}
