package com.geminiflow.app.data.api.streaming

import com.geminiflow.app.domain.model.export.ImageExportMetadata

/**
 * Gemini 串流協定訊框強型別模型。
 * 遵循單趟解析原則，將原始位元流單行資料轉換為明確的不可變領域訊框。
 */
sealed interface StreamFrame {

    /**
     * 文字增量訊框
     * @param delta 本次訊框提取出的增量文字（保留所有空格與換行）
     * @param conversationId 會話 Session ID (c_...)
     * @param continuationToken 會話續接 Token (Aw...)
     */
    data class Text(
        val delta: String,
        val conversationId: String? = null,
        val continuationToken: String? = null
    ) : StreamFrame

    /**
     * 生圖中繼資料訊框（用於高解析原圖導出）
     */
    data class ImageExport(
        val metadata: ImageExportMetadata,
        val conversationId: String? = null,
        val continuationToken: String? = null
    ) : StreamFrame

    /**
     * 會話初始化訊框（僅包含會話上下文，尚未有內文或生圖）
     */
    data class SessionContext(
        val conversationId: String,
        val continuationToken: String? = null
    ) : StreamFrame

    /**
     * 串流終止訊框（包含 e 或 di 結束標記）
     */
    data object EndOfStream : StreamFrame

    /**
     * 忽略訊框（如長度前綴數字、心跳或格式不符之行）
     */
    data object Ignored : StreamFrame
}
