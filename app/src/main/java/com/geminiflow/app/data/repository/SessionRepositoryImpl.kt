package com.geminiflow.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.geminiflow.app.domain.model.auth.SessionData
import com.geminiflow.app.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SessionRepositoryImpl(
    context: Context
) : SessionRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gemini_flow_sessions", Context.MODE_PRIVATE)

    override suspend fun getSession(sessionId: String): SessionData? = withContext(Dispatchers.IO) {
        val jsonStr = prefs.getString(sessionId, null) ?: return@withContext null
        try {
            val obj = JSONObject(jsonStr)
            val conversationId = obj.optString("conversationId", "").takeIf { it.isNotEmpty() }
            val continuationToken = obj.optString("continuationToken", "").takeIf { it.isNotEmpty() }
            val turnIndex = obj.optInt("turnIndex", 0)
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

            return@withContext SessionData(
                sessionId = sessionId,
                conversationId = conversationId,
                continuationToken = continuationToken,
                turnIndex = turnIndex,
                updatedAt = updatedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveSession(sessionData: SessionData): Unit = withContext(Dispatchers.IO) {
        val obj = JSONObject().apply {
            put("sessionId", sessionData.sessionId)
            put("conversationId", sessionData.conversationId ?: JSONObject.NULL)
            put("continuationToken", sessionData.continuationToken ?: JSONObject.NULL)
            put("turnIndex", sessionData.turnIndex)
            put("updatedAt", sessionData.updatedAt)
        }
        prefs.edit().putString(sessionData.sessionId, obj.toString()).apply()
    }

    override suspend fun clearSession(sessionId: String): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove(sessionId).apply()
    }
}
