package com.geminiflow.app.data.repository

import com.geminiflow.app.data.datasource.gallery.MediaStoreGalleryDataSource
import com.geminiflow.app.domain.model.gallery.GalleryMediaItem
import com.geminiflow.app.domain.repository.GalleryRepository

class GalleryRepositoryImpl(
    private val dataSource: MediaStoreGalleryDataSource
) : GalleryRepository {

    override suspend fun getMediaPaged(offset: Int, limit: Int): List<GalleryMediaItem> {
        return dataSource.queryMedia(offset = offset, limit = limit)
    }
}
