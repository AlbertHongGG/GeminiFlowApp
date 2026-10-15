package com.geminiflow.app.fakes

import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.domain.repository.ImageUploader

/**
 * 測試專用的 In-Memory Fake 圖片上傳器。
 * 模擬上傳回傳值並記錄呼叫歷史，避免單元測試依賴真實網路。
 */
class FakeImageUploader(
    private val stubbedRefPrefix: String = "/contrib_service/ttl_1d/fake_ref"
) : ImageUploader {

    val uploadedImages = mutableListOf<ImagePayload>()
    var capturedCookies: Map<String, String>? = null
    var shouldFailWith: Exception? = null

    override suspend fun uploadImages(
        images: List<ImagePayload>,
        cookies: Map<String, String>
    ): List<Pair<String, String>> {
        shouldFailWith?.let { throw it }

        uploadedImages.addAll(images)
        capturedCookies = cookies

        return images.mapIndexed { index, img ->
            val filename = img.filename.takeIf { it.isNotBlank() } ?: "image_${index + 1}.jpg"
            Pair("${stubbedRefPrefix}_${index + 1}", filename)
        }
    }
}
