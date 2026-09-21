package com.geminiflow.app.data.api

object GeminiConfig {
    const val GEMINI_BASE_URL = "https://gemini.google.com"
    const val GEMINI_REQUEST_URL =
        "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate"
    const val UPLOAD_IMAGE_URL = "https://content-push.googleapis.com/upload/"

    const val REQUEST_BL_PARAM = "boq_assistant-bard-web-server_20260618.10_p0"
    const val REQUIRED_COOKIE_NAME = "__Secure-1PSID"
    const val GOOGLE_COOKIE_DOMAIN = ".google.com"

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val DEFAULT_HTTP_HEADERS = mapOf(
        "authority" to "gemini.google.com",
        "origin" to "https://gemini.google.com",
        "referer" to "https://gemini.google.com/",
        "x-same-domain" to "1",
        "user-agent" to DEFAULT_USER_AGENT
    )

    val UPLOAD_IMAGE_HEADERS = mapOf(
        "authority" to "content-push.googleapis.com",
        "accept" to "*/*",
        "accept-language" to "en-US,en;q=0.7",
        "authorization" to "Basic c2F2ZXM6cyNMdGhlNmxzd2F2b0RsN3J1d1U=",
        "content-type" to "application/x-www-form-urlencoded;charset=UTF-8",
        "origin" to "https://gemini.google.com",
        "push-id" to "feeds/mcudyrk2a4khkz",
        "referer" to "https://gemini.google.com/",
        "x-goog-upload-command" to "start",
        "x-goog-upload-header-content-length" to "",
        "x-goog-upload-protocol" to "resumable",
        "x-tenant-id" to "bard-storage"
    )

    val MODEL_HEADERS = mapOf(
        "gemini-3-pro" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"e6fa609c3fa255c0\",null,null,0,[4,5,6,8],null,null,2,null,null,3,1,\"{ext_uuid}\"]"
        ),
        "gemini-3.5-flash" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"56fdd199312815e2\",null,null,0,[4,5,6,8],null,null,2,null,null,1,1,\"{ext_uuid}\"]"
        ),
        "gemini-3.6-flash" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"56fdd199312815e2\",null,null,0,[4,5,6,8],null,null,2,null,null,1,1,\"{ext_uuid}\"]"
        ),
        "gemini-3.6-flash-thinking" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"56fdd199312815e2\",null,null,0,[4,5,6,8],null,null,2,null,null,1,2,\"{ext_uuid}\"]"
        ),
        "gemini-3.7-flash" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"56fdd199312815e2\",null,null,0,[4,5,6,8],null,null,2,null,null,1,1,\"{ext_uuid}\"]"
        ),
        "gemini-3.7-flash-thinking" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"56fdd199312815e2\",null,null,0,[4,5,6,8],null,null,2,null,null,1,2,\"{ext_uuid}\"]"
        ),
        "gemini-3-pro-image-preview" to mapOf(
            "x-goog-ext-525001261-jspb" to "[1,null,null,null,\"e6fa609c3fa255c0\",null,null,0,[4,5,6,8],null,null,2,null,null,3,2,\"{ext_uuid}\"]"
        )
    )
}
