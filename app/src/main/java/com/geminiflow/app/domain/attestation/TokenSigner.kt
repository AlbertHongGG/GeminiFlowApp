package com.geminiflow.app.domain.attestation

import com.geminiflow.app.domain.model.attestation.BotguardToken

/**
 * 負責產生 Google Web Attestation (Botguard) 內容綁定安全權杖的抽象契約。
 */
interface TokenSigner {
    /**
     * 針對特定的提示詞內容 (effectivePrompt) 與會話 ID 簽章產生 BotguardToken。
     * 若簽章失敗或環境不支援，可回傳 null 進行優雅降級。
     */
    suspend fun sign(effectivePrompt: String, conversationId: String? = null): BotguardToken?
}
