package com.geminiflow.app.domain.model.gallery

import android.net.Uri

data class GalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateAddedSec: Long,
    val size: Long
)
