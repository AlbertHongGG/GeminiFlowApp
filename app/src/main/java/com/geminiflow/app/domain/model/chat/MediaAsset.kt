package com.geminiflow.app.domain.model.chat

import com.geminiflow.app.domain.model.export.ImageExportMetadata
import java.io.File

sealed interface MediaAsset {
    val id: String
    val rawUrl: String

    data class LocalReady(
        override val id: String,
        override val rawUrl: String,
        val localFile: File,
        val mimeType: String = "image/png",
        val sizeBytes: Long = localFile.length()
    ) : MediaAsset

    data class Downloading(
        override val id: String,
        override val rawUrl: String,
        val progress: Float = 0f,
        val exportMetadata: ImageExportMetadata? = null
    ) : MediaAsset

    data class Failed(
        override val id: String,
        override val rawUrl: String,
        val errorMessage: String,
        val canRetry: Boolean = true,
        val exportMetadata: ImageExportMetadata? = null
    ) : MediaAsset
}
