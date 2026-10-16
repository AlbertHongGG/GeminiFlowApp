package com.geminiflow.app.data.api

object GeminiConfig {
    const val GEMINI_BASE_URL = "https://gemini.google.com"
    const val GEMINI_APP_URL = "https://gemini.google.com/app"
    const val GEMINI_REQUEST_URL =
        "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate"
    const val UPLOAD_IMAGE_URL = "https://push.clients6.google.com/upload/"

    const val REQUEST_BL_PARAM = "boq_gemini-web-uiserver_20261012.19_p1"
    const val REQUIRED_COOKIE_NAME = "__Secure-1PSID"
    const val GOOGLE_COOKIE_DOMAIN = ".google.com"

    const val WAA_CREATE_URL = "https://waa-pa.clients6.google.com/\$rpc/google.internal.waa.v1.Waa/Create"
    const val WAA_API_KEY = "AIzaSyBGb5fGAyC-pRcRU6MUHb__b_vKha71HRE"
    const val GEMINI_WAA_SERVICE_KEY = "br1aemAN9owlYRs9NnsA"

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val DEFAULT_HTTP_HEADERS = mapOf(
        "authority" to "gemini.google.com",
        "origin" to "https://gemini.google.com",
        "referer" to "https://gemini.google.com/",
        "x-same-domain" to "1",
        "sec-fetch-dest" to "empty",
        "sec-fetch-mode" to "cors",
        "sec-fetch-site" to "same-origin",
        "user-agent" to DEFAULT_USER_AGENT
    )

    val BROWSER_NAVIGATION_HEADERS = mapOf(
        "authority" to "gemini.google.com",
        "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "accept-language" to "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "referer" to "https://gemini.google.com/",
        "sec-fetch-dest" to "document",
        "sec-fetch-mode" to "navigate",
        "sec-fetch-site" to "same-origin",
        "sec-fetch-user" to "?1",
        "upgrade-insecure-requests" to "1",
        "user-agent" to DEFAULT_USER_AGENT
    )

    val UPLOAD_IMAGE_HEADERS = mapOf(
        "authority" to "push.clients6.google.com",
        "accept" to "*/*",
        "accept-language" to "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "content-type" to "application/x-www-form-urlencoded;charset=UTF-8",
        "origin" to "https://gemini.google.com",
        "push-id" to "feeds/mcudyrk2a4khkz",
        "referer" to "https://gemini.google.com/",
        "x-goog-upload-protocol" to "resumable",
        "x-tenant-id" to "bard-storage"
    )
}
