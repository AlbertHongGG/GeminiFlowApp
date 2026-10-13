package com.geminiflow.app.data.api.protocol

import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import java.util.UUID
import kotlin.random.Random

class GeminiProtocolEngine(
    val serverBl: String
) {
    val sessionUuid: String = UUID.randomUUID().toString().uppercase()

    fun buildRequest(
        request: ChatRequest,
        tokens: GeminiTokens,
        uploads: List<Pair<String, String>> = emptyList(),
        conversationIds: List<String> = emptyList(),
        turnIndex: Int = 0
    ): ProtocolRequest {
        val reqUuid = UUID.randomUUID().toString().uppercase()
        val modelSpec = request.modelSpec

        // 1. URL Query Parameters
        val params = mapOf(
            "bl" to serverBl,
            "hl" to request.language,
            "_reqid" to Random.nextInt(111111, 9999999).toString(),
            "rt" to "c",
            "f.sid" to (tokens.sid ?: "")
        )

        // 2. JSPB Extension Headers
        val headerBuilder = JspbHeaderBuilder(
            modelSpec = modelSpec,
            sessionUuid = sessionUuid,
            reqUuid = reqUuid,
            serverBl = serverBl
        )
        val headers = headerBuilder.buildAllHeaders()

        // 3. Payload Post Data
        val payloadBuilder = GeminiPayloadBuilder(
            prompt = request.prompt,
            systemPrompt = request.systemPrompt,
            language = request.language,
            modelSpec = modelSpec,
            reqUuid = reqUuid,
            uploads = uploads,
            conversationIds = conversationIds,
            turnIndex = turnIndex
        )
        val postData = payloadBuilder.buildPostData(atToken = tokens.snlm0e)

        return ProtocolRequest(
            params = params,
            headers = headers,
            postData = postData
        )
    }
}
