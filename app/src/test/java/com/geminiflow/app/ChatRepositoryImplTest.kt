package com.geminiflow.app

import com.geminiflow.app.data.repository.ChatRepositoryImpl
import com.geminiflow.app.domain.model.auth.GeminiTokens
import com.geminiflow.app.domain.model.chat.ChatRequest
import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.fakes.FakeImageUploader
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryImplTest {

    @Test
    fun testStreamGenerate_DelegatesImageUploadToImageUploader() = runBlocking {
        val fakeUploader = FakeImageUploader(stubbedRefPrefix = "/contrib_service/ttl_1d/test_ref")
        var postDataCaptured: String? = null

        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val buffer = okio.Buffer()
            req.body?.writeTo(buffer)
            postDataCaptured = buffer.readUtf8()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body("""data: [["wrb.fr",null,"[[\"c_123\",null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null]"]]""".toResponseBody("text/event-stream".toMediaType()))
                .build()
        }).build()

        val repository = ChatRepositoryImpl(
            client = testClient,
            imageUploader = fakeUploader
        )

        val image = ImagePayload(data = byteArrayOf(1, 2, 3), filename = "test_doc.jpg")
        val request = ChatRequest(
            prompt = "請分析此文件",
            images = listOf(image),
            model = "gemini-2.0-flash"
        )
        val tokens = GeminiTokens(snlm0e = "mock_at")
        val cookies = mapOf("__Secure-1PSID" to "mock_cookie_sid")

        repository.streamGenerate(request, tokens, cookies, null).firstOrNull()

        // 驗證圖片上傳器被正確調用
        assertEquals(1, fakeUploader.uploadedImages.size)
        assertEquals("test_doc.jpg", fakeUploader.uploadedImages[0].filename)
        assertEquals(cookies, fakeUploader.capturedCookies)

        // 驗證 JSPB 對話負載包含 FakeUploader 回傳之 upload_ref
        assertNotNull(postDataCaptured)
        val decodedBody = java.net.URLDecoder.decode(postDataCaptured, "UTF-8")
        assertTrue(decodedBody.contains("/contrib_service/ttl_1d/test_ref_1"))
        assertTrue(decodedBody.contains("test_doc.jpg"))
    }

    @Test
    fun testStreamGenerate_WithoutImages_DoesNotCallImageUploader() = runBlocking {
        val fakeUploader = FakeImageUploader()

        val testClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body("".toResponseBody("text/event-stream".toMediaType()))
                .build()
        }).build()

        val repository = ChatRepositoryImpl(
            client = testClient,
            imageUploader = fakeUploader
        )

        val request = ChatRequest(
            prompt = "Hello",
            images = emptyList(),
            model = "gemini-2.0-flash"
        )
        repository.streamGenerate(request, GeminiTokens(snlm0e = "mock"), emptyMap(), null).firstOrNull()

        assertEquals(0, fakeUploader.uploadedImages.size)
    }
}
