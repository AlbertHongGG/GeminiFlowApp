package com.geminiflow.app.domain.model.attestation
 
data class BotguardToken(
    val token: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis()
)
