package com.geminiflow.app.domain.model.export

/**
 * Google Gemini 圖片導出中繼資料數值物件（Value Object）。
 * 完整封裝從 SSE 串流結構化節點中提取的會話識別碼、回應識別碼、候選識別碼與圖片區塊樹狀結構，
 * 用以透過 c8o8Fe RPC 進行 2816x1536 超高解析原圖導出。
 */
data class ImageExportMetadata(
    val conversationId: String,
    val responseId: String,
    val choiceId: String,
    val imageId: String,
    val imageBlockJson: String,
    val previewUrl: String
) {
    init {
        require(conversationId.isNotBlank()) { "conversationId 不可為空" }
        require(responseId.isNotBlank()) { "responseId 不可為空" }
        require(choiceId.isNotBlank()) { "choiceId 不可為空" }
        require(imageId.isNotBlank()) { "imageId 不可為空" }
        require(imageBlockJson.isNotBlank()) { "imageBlockJson 不可為空" }
        require(previewUrl.isNotBlank()) { "previewUrl 不可為空" }
    }
}
