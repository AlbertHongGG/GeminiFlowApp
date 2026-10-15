package com.geminiflow.app.domain.model.gallery

data class MediaMonthGroup(
    val monthKey: String,
    val items: List<GalleryMediaItem>
)
