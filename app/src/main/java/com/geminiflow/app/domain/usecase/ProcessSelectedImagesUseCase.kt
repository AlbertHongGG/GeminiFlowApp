package com.geminiflow.app.domain.usecase

import android.net.Uri
import com.geminiflow.app.data.media.ImageCompressor
import com.geminiflow.app.domain.model.chat.ImagePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProcessSelectedImagesUseCase(
    private val imageCompressor: ImageCompressor
) {
    suspend operator fun invoke(uris: List<Uri>): List<ImagePayload> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            imageCompressor.compressUri(uri)
        }
    }
}
