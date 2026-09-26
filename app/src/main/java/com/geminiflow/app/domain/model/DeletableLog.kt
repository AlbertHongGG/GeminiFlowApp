package com.geminiflow.app.domain.model

import java.io.File

interface DeletableLog {
    val id: String
    val file: File
    val timestampMillis: Long
}
