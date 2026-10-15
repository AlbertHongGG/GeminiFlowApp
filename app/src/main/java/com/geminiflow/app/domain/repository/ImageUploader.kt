package com.geminiflow.app.domain.repository

import com.geminiflow.app.domain.model.chat.ImagePayload

/**
 * 圖片上傳服務領域合約 (Domain Contract)。
 * 專責處理多模態（Multimodal）圖片資料至 Google Scotty Resumable Upload 服務之傳輸。
 */
interface ImageUploader {

    /**
     * 上傳多模態圖片二進位資料並取得儲存參照識別碼 (Upload Reference ID)。
     *
     * @param images 欲上傳之圖片清單
     * @param cookies Google 身分驗證 Cookie 鍵值對
     * @return 成功上傳之 (upload_ref, filename) 清單，供注入至對話負載中使用
     * @throws com.geminiflow.app.domain.model.common.NetworkException 上傳連線失敗或伺服器拒絕時顯性拋出
     */
    suspend fun uploadImages(
        images: List<ImagePayload>,
        cookies: Map<String, String>
    ): List<Pair<String, String>>
}
