package com.geminiflow.app.domain.model.auth

/**
 * 領域層強型別工作階段狀態。
 * 遵循單一事實來源原則，嚴格反映本機 Session 憑證的實體存在性。
 */
sealed interface SessionState {
    /**
     * 已通過 Google 身分驗證授權（本機持有 __Secure-1PSID）。
     */
    data class Authenticated(val accountHint: String? = null) : SessionState

    /**
     * 未登入或已明確登出。
     */
    data object Unauthenticated : SessionState
}
