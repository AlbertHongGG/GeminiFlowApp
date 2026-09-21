package com.geminiflow.app.data.auth.browser

object AuthBrowserConfig {

    const val INITIAL_LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?service=mail&continue=https%3A%2F%2Fgemini.google.com%2Fapp"

    const val GEMINI_APP_URL = "https://gemini.google.com/app"
    const val GEMINI_DOMAIN = "gemini.google.com"
    const val GOOGLE_ACCOUNTS_DOMAIN = "accounts.google.com"

    const val REQUIRED_COOKIE_NAME = "__Secure-1PSID"

    // Standard high-compatibility Mobile Safari User-Agent (Google never blocks WebKit Safari)
    const val SAFARI_IOS_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

    fun buildCleanMobileChromeUa(defaultUa: String): String {
        var cleanUa = defaultUa
            .replace(Regex(";\\s*wv\\b"), "")
            .replace(Regex("Version/\\d+\\.\\d+\\s+"), "")
            .trim()

        if (!cleanUa.contains("Chrome/")) {
            cleanUa = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        }
        return cleanUa
    }
}
