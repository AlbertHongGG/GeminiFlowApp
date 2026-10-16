package com.geminiflow.app.data.auth

/**
 * 本機身分憑證儲存庫介面。
 * 負責 Google 核心 Session Cookie 的持久化讀寫與檢查，不發起任何 HTTP 網路通訊。
 */
interface CredentialStore {
    /**
     * 獲取本機存儲之所有 Google 憑證映射表。
     */
    fun getCookies(): Map<String, String>

    /**
     * 檢查本機是否持有有效且必要的 Google 登入憑證 (__Secure-1PSID)。
     */
    fun hasRequiredCredential(): Boolean

    /**
     * 持久化寫入 Cookie 字串至本機儲存庫。
     */
    fun saveCookies(cookiesHeader: String)

    /**
     * 清空本機存儲之所有 Google 憑證。
     */
    fun clearCredentials()
}
