package com.geminiflow.app.domain.model

/**
 * 串流對話輸出片段模型。
 * 支援文字增量、會話識別碼以及強型別多媒體資產 (MediaAsset)。
 */
data class ChatResponseChunk(
    val text: String? = null,
    val imageUrl: String? = null,
    val mediaAsset: MediaAsset? = null,
    val sessionIds: List<String>? = null
)
