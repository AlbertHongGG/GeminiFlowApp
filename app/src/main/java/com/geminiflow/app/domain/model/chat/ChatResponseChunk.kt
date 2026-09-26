package com.geminiflow.app.domain.model.chat

import com.geminiflow.app.domain.model.export.ImageExportMetadata

/**
 * 對話串流回應資料區塊。
 */
data class ChatResponseChunk(
    val text: String? = null,
    val imageUrl: String? = null,
    val mediaAsset: MediaAsset? = null,
    val sessionIds: List<String>? = null,
    val exportMetadata: ImageExportMetadata? = null
)
