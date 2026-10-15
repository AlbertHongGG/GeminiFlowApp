package com.geminiflow.app

import com.geminiflow.app.data.network.upload.GeminiImageUploader
import com.geminiflow.app.domain.model.chat.ImagePayload
import com.geminiflow.app.domain.model.common.NetworkException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeminiImageUploaderTest {

    @Test
    fun testUploadSuccess_WithHttp2LowercaseHeaders() = runBlocking {
        val recordedRequests = mutableListOf<Request>()
        val recordedBodies = mutableListOf<ByteArray>()

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            recordedRequests.add(req)
            val buffer = Buffer()
            req.body?.writeTo(buffer)
            recordedBodies.add(buffer.readByteArray())

            if (req.url.toString().contains("upload_id=")) {
                // Step 2: Binary finalize
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .body("/contrib_service/ttl_1d/uploaded_ref_abc123\n".toResponseBody("text/html".toMediaType()))
                    .build()
            } else {
                // Step 1: Start Session (HTTP/2 lowercase headers)
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .header("x-goog-upload-url", "https://push.clients6.google.com/upload/?upload_id=mock_session_1")
                    .header("x-goog-upload-status", "active")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val uploader = GeminiImageUploader(testClient)
        val testData = "fake_binary_jpeg_bytes".toByteArray(Charsets.UTF_8)
        val images = listOf(ImagePayload(data = testData, filename = "receipt.jpg"))
        val cookies = mapOf("__Secure-1PSID" to "mock_cookie_val")

        val results = uploader.uploadImages(images, cookies)

        assertEquals(1, results.size)
        val (ref, filename) = results[0]
        assertEquals("/contrib_service/ttl_1d/uploaded_ref_abc123", ref)
        assertEquals("receipt.jpg", filename)

        // 驗證 Step 1 標頭與資料
        assertEquals(2, recordedRequests.size)
        val step1Req = recordedRequests[0]
        assertEquals("start", step1Req.header("x-goog-upload-command"))
        assertEquals(testData.size.toString(), step1Req.header("x-goog-upload-header-content-length"))
        assertEquals("__Secure-1PSID=mock_cookie_val", step1Req.header("Cookie"))
        assertEquals("File name: receipt.jpg", recordedBodies[0].toString(Charsets.UTF_8))

        // 驗證 Step 2 標頭與資料
        val step2Req = recordedRequests[1]
        assertEquals("https://push.clients6.google.com/upload/?upload_id=mock_session_1", step2Req.url.toString())
        assertEquals("upload, finalize", step2Req.header("x-goog-upload-command"))
        assertEquals("0", step2Req.header("x-goog-upload-offset"))
        assertTrue(testData.contentEquals(recordedBodies[1]))
    }

    @Test
    fun testUploadSuccess_WithTitleCaseHeaders() = runBlocking {
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.url.toString().contains("upload_id=")) {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("/contrib_service/ttl_1d/title_case_ref".toResponseBody("text/html".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("X-Goog-Upload-Url", "https://push.clients6.google.com/upload/?upload_id=mock_title_case")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)
        val images = listOf(ImagePayload(data = byteArrayOf(1, 2, 3), filename = "sample.png"))

        val results = uploader.uploadImages(images, emptyMap())
        assertEquals(1, results.size)
        assertEquals("/contrib_service/ttl_1d/title_case_ref", results[0].first)
    }

    @Test
    fun testUploadStep1_Non200_ThrowsNetworkException() = runBlocking {
        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(403)
                .message("Forbidden")
                .body("Access denied to Scotty upload service".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)

        try {
            uploader.uploadImages(listOf(ImagePayload(byteArrayOf(1), "test.jpg")), emptyMap())
            fail("應當拋出 NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message?.contains("403") == true)
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun testUploadStep1_MissingUploadUrlHeader_ThrowsNetworkException() = runBlocking {
        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body("OK without header".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)

        try {
            uploader.uploadImages(listOf(ImagePayload(byteArrayOf(1), "test.jpg")), emptyMap())
            fail("應當拋出 NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message?.contains("x-goog-upload-url") == true)
        }
    }

    @Test
    fun testUploadStep2_Non200_ThrowsNetworkException() = runBlocking {
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.url.toString().contains("upload_id=")) {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(500)
                    .message("Internal Server Error")
                    .body("Upload server crashed".toResponseBody("text/plain".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .header("x-goog-upload-url", "https://push.clients6.google.com/upload/?upload_id=ok")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)

        try {
            uploader.uploadImages(listOf(ImagePayload(byteArrayOf(1), "test.jpg")), emptyMap())
            fail("應當拋出 NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message?.contains("500") == true)
            assertTrue(e.message?.contains("Upload server crashed") == true)
        }
    }

    @Test
    fun testUploadStep2_EmptyUploadRef_ThrowsNetworkException() = runBlocking {
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            if (req.url.toString().contains("upload_id=")) {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .body("   ".toResponseBody("text/plain".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .header("x-goog-upload-url", "https://push.clients6.google.com/upload/?upload_id=ok")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)

        try {
            uploader.uploadImages(listOf(ImagePayload(byteArrayOf(1), "test.jpg")), emptyMap())
            fail("應當拋出 NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message?.contains("空白") == true)
        }
    }

    @Test
    fun testUploadMultipleImages_Sequential() = runBlocking {
        var callCount = 0
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            callCount++
            if (req.url.toString().contains("upload_id=")) {
                val id = req.url.queryParameter("upload_id") ?: "0"
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .body("/contrib_service/ttl_1d/ref_$id".toResponseBody("text/plain".toMediaType()))
                    .build()
            } else {
                val currentIdx = (callCount + 1) / 2
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_2)
                    .code(200)
                    .message("OK")
                    .header("x-goog-upload-url", "https://push.clients6.google.com/upload/?upload_id=$currentIdx")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val uploader = GeminiImageUploader(testClient)

        val images = listOf(
            ImagePayload(byteArrayOf(1), "pic1.jpg"),
            ImagePayload(byteArrayOf(2), "pic2.png")
        )

        val results = uploader.uploadImages(images, emptyMap())
        assertEquals(2, results.size)
        assertEquals("/contrib_service/ttl_1d/ref_1", results[0].first)
        assertEquals("pic1.jpg", results[0].second)
        assertEquals("/contrib_service/ttl_1d/ref_2", results[1].first)
        assertEquals("pic2.png", results[1].second)
        assertEquals(4, callCount)
    }

    @Test
    fun testUploadEmptyList_ReturnsEmptyImmediately() = runBlocking {
        var callCount = 0
        val testClient = OkHttpClient.Builder().addInterceptor { chain ->
            callCount++
            chain.proceed(chain.request())
        }.build()

        val uploader = GeminiImageUploader(testClient)
        val results = uploader.uploadImages(emptyList(), emptyMap())
        assertTrue(results.isEmpty())
        assertEquals(0, callCount)
    }
}
