package com.geminiflow.app.domain.model.auth

data class SessionData(
    val sessionId: String,
    val conversationId: String? = null,
    val continuationToken: String? = null,
    val turnIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isNew: Boolean
        get() = turnIndex == 0 || conversationId.isNullOrBlank()

    fun advanceTurn(continuationToken: String? = null): SessionData {
        return copy(
            turnIndex = turnIndex + 1,
            continuationToken = continuationToken ?: this.continuationToken,
            updatedAt = System.currentTimeMillis()
        )
    }
}
