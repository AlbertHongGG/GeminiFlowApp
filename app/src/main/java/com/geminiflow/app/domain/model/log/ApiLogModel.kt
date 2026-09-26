package com.geminiflow.app.domain.model.log

import java.io.File

data class ApiLogModel(
    override val id: String,
    override val file: File,
    val title: String,
    val displayTime: String,
    val durationMs: Long,
    val timestamp: Long,
    val rawJson: String,
    val source: String = "全部"
) : DeletableLog {
    override val timestampMillis: Long get() = timestamp
}
