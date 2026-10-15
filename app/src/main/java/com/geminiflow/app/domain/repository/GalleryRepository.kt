package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.gallery.GalleryMediaItem

interface GalleryRepository {
    suspend fun getMediaPaged(offset: Int, limit: Int): List<GalleryMediaItem>
}
