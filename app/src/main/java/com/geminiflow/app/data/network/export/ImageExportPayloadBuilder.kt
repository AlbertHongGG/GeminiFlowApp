package com.geminiflow.app.data.network.export

import com.geminiflow.app.data.api.payload.JSONObjectCompat
import com.geminiflow.app.domain.model.export.ImageExportMetadata
import org.json.JSONArray

/**
 * Google Gemini c8o8Fe 原圖導出 Batchexecute 負載建構器。
 * 負責將 ImageExportMetadata 組裝成 Google 伺服器嚴格要求的 JSON 陣列結構。
 */
class ImageExportPayloadBuilder(
    private val correlationIdGenerator: ExportCorrelationIdGenerator = ExportCorrelationIdGenerator()
) {
    companion object {
        const val RPC_ID = "c8o8Fe"
    }

    data class BuiltPayload(
        val outerJson: String,
        val correlationId: String
    )

    fun build(metadata: ImageExportMetadata): BuiltPayload {
        val correlationId = correlationIdGenerator.generate(16)
        val imgBlock = JSONArray(metadata.imageBlockJson)

        // 構造 imgBlock[1] 之 [url, 0] 格式
        val imgBlock1 = imgBlock.optJSONArray(1)
        val targetUrlStr = imgBlock1?.optString(0, "") ?: ""
        val node1Formatted = JSONArray().apply {
            put(targetUrlStr)
            put(0)
        }

        // 內部第一個陣列:
        // [
        //   imgBlock[0],
        //   [imgBlock[1][0], 0],
        //   null,
        //   imgBlock[3],
        //   null, null, null, null,
        //   imgBlock[8],
        //   correlationId
        // ]
        val innerPart0 = JSONArray().apply {
            put(imgBlock.opt(0) ?: JSONObjectCompat.NULL)
            put(node1Formatted)
            put(JSONObjectCompat.NULL)
            put(imgBlock.opt(3) ?: JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(JSONObjectCompat.NULL)
            put(imgBlock.optString(8, metadata.imageId))
            put(correlationId)
        }

        // 內部第二個陣列: [responseId, choiceId, conversationId, null, correlationId]
        val innerPart1 = JSONArray().apply {
            put(metadata.responseId)
            put(metadata.choiceId)
            put(metadata.conversationId)
            put(JSONObjectCompat.NULL)
            put(correlationId)
        }

        // 完整內部參數: [innerPart0, innerPart1, 1, 0, 1]
        val innerPayload = JSONArray().apply {
            put(innerPart0)
            put(innerPart1)
            put(1)
            put(0)
            put(1)
        }

        // 外層 Batchexecute 包裹: [[["c8o8Fe", innerPayloadString, null, "generic"]]]
        val rpcItem = JSONArray().apply {
            put(RPC_ID)
            put(innerPayload.toString())
            put(JSONObjectCompat.NULL)
            put("generic")
        }

        val outerReq = JSONArray().apply {
            put(JSONArray().apply { put(rpcItem) })
        }

        return BuiltPayload(
            outerJson = outerReq.toString(),
            correlationId = correlationId
        )
    }
}
