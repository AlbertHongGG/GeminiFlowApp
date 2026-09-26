package com.geminiflow.app.data.network.export

import kotlin.random.Random

/**
 * 圖片導出客戶端隨機追蹤標識產生器。
 * 生成符合 Google 導出協定要求的 16 碼英數隨機字串。
 */
class ExportCorrelationIdGenerator {
    private val chars = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun generate(length: Int = 16): String {
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
