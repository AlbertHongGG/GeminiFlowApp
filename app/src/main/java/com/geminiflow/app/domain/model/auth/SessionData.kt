package com.geminiflow.app.domain.model.auth

data class SessionData(
    val sessionId: String,
    val conversationIds: List<String>
)
