package com.geminiflow.app.server.routes

import android.util.Base64
import com.geminiflow.app.data.storage.TrafficLogManager
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.domain.model.common.PayloadException
import com.geminiflow.app.domain.model.log.TrafficLog
import com.geminiflow.app.server.dto.ChatRequestDto

object RouteUtils {
    fun recordErrorTraffic(
        trafficLogManager: TrafficLogManager,
        method: String,
        path: String,
        statusCode: Int,
        startTime: Long,
        clientIp: String,
        prompt: String?,
        error: String?
    ) {
        trafficLogManager.record(
            TrafficLog(
                method = method,
                path = path,
                statusCode = statusCode,
                durationMs = System.currentTimeMillis() - startTime,
                clientIp = clientIp,
                promptSummary = prompt,
                responseSummary = error ?: "HTTP $statusCode"
            )
        )
    }

    fun mapToDomainRequest(dto: ChatRequestDto): ChatRequest {
        val model = dto.model?.takeIf { it.isNotBlank() }
            ?: throw PayloadException("缺少模型參數 (model)，呼叫端必須明確指定欲使用的模型")

        val imagePayloads = dto.images.mapIndexed { index, rawStr ->
            decodeBase64Image(rawStr, index)
        }

        return ChatRequest(
            prompt = dto.prompt,
            systemPrompt = dto.systemPrompt,
            model = model,
            language = dto.language,
            images = imagePayloads,
            sessionId = dto.sessionId,
            autoRefreshCookies = dto.autoRefreshCookies
        )
    }

    private fun decodeBase64Image(value: String, index: Int): ImagePayload {
        var base64Part = value
        var ext = "png"

        if (value.startsWith("data:image/")) {
            val commaIndex = value.indexOf(",")
            if (commaIndex != -1) {
                val header = value.substring(0, commaIndex)
                base64Part = value.substring(commaIndex + 1)
                val mime = header.substringAfter("data:").substringBefore(";")
                ext = when (mime.lowercase()) {
                    "image/jpeg", "image/jpg" -> "jpg"
                    "image/webp" -> "webp"
                    else -> "png"
                }
            }
        }

        val cleaned = base64Part.replace("\\s+".toRegex(), "")
        val bytes = Base64.decode(cleaned, Base64.DEFAULT)
        return ImagePayload(data = bytes, filename = "upload_${index + 1}.$ext")
    }
}
