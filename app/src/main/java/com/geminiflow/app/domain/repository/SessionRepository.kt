package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.auth.SessionData

interface SessionRepository {
    suspend fun getSession(sessionId: String): SessionData?
    suspend fun saveSession(sessionData: SessionData)
    suspend fun clearSession(sessionId: String)
}
