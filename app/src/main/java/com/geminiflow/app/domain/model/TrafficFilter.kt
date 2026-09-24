package com.geminiflow.app.domain.model

enum class TrafficFilter(val label: String) {
    ALL("全部"),
    STREAM_ONLY("僅串流 (/stream)"),
    ERROR_ONLY("僅錯誤")
}
