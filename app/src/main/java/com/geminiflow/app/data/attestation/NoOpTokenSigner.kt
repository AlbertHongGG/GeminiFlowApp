package com.geminiflow.app.data.attestation

import com.geminiflow.app.domain.attestation.TokenSigner
import com.geminiflow.app.domain.model.attestation.BotguardToken

/**
 * 空操作 Token 簽名器，供單元測試或不支援 JS 執行之環境做容錯降級使用。
 */
class NoOpTokenSigner : TokenSigner {
    override suspend fun sign(effectivePrompt: String, conversationId: String?): BotguardToken? = null
}
