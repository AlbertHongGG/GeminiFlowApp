package com.geminiflow.app.domain.model

import java.io.File

/**
 * 領域多媒體資產強型別狀態機。
 * 遵循 OOP 原則，徹底消除 Primitive Obsession，封裝本地路徑、遠端 URL 與生命週期狀態。
 */
sealed interface MediaAsset {
    val id: String
    val rawUrl: String

    /**
     * 影像已成功落地並儲存至本機快取。
     * UI 直接載入 localFile，保證 100% 渲染且不受 CDN Cookie 或網路波動影響。
     */
    data class LocalReady(
        override val id: String,
        override val rawUrl: String,
        val localFile: File,
        val mimeType: String = "image/png",
        val sizeBytes: Long = localFile.length()
    ) : MediaAsset

    /**
     * 影像正在下載中。
     * UI 呈現 Shimmer 骨架屏與進度指示。
     */
    data class Downloading(
        override val id: String,
        override val rawUrl: String,
        val progress: Float = 0f
    ) : MediaAsset

    /**
     * 影像下載或解碼失敗。
     * 明確攜帶失敗診斷原因與重試機制，徹底消滅無聲失敗與空白對話框。
     */
    data class Failed(
        override val id: String,
        override val rawUrl: String,
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : MediaAsset
}
