package com.geminiflow.app.domain.model

data class SessionData(
    val sessionId: String,
    val conversationIds: List<String>
)
