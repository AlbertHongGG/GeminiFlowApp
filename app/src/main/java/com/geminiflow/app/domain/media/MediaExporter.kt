package com.geminiflow.app.domain.media

import java.io.File

interface MediaExporter {
    suspend fun exportImageToGallery(file: File, displayName: String? = null): Result<String>
}
