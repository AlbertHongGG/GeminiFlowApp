package com.geminiflow.app.data.api.protocol

data class ProtocolRequest(
    val params: Map<String, String>,
    val headers: Map<String, String>,
    val postData: Map<String, String>
)
