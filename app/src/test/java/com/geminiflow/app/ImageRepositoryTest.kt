package com.geminiflow.app

import com.geminiflow.app.data.repository.ImageRepositoryImpl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val stubContext = object : android.content.ContextWrapper(null) {
        override fun getFilesDir(): File = tempFolder.root
    }

    @Test
    fun testBase64Decoding_WritesDirectlyToFile() = kotlinx.coroutines.runBlocking {
        val repository = ImageRepositoryImpl(
            context = stubContext,
            imagesDirProvider = { tempFolder.root }
        )

        val base64Data = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        val file = repository.downloadImage(
            url = base64Data,
            modelName = "gemini-3-pro"
        )

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.contains("gemini-3-pro_generated.png"))
    }

    @Test
    fun testHttpDownload_SendsBrowserHeadersAndAtomicSave() = kotlinx.coroutines.runBlocking {
        val fakeImageData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        var recordedRequest: Request? = null

        val mockInterceptor = Interceptor { chain ->
            val req = chain.request()
            recordedRequest = req
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(fakeImageData.toResponseBody("image/png".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = ImageRepositoryImpl(
            context = stubContext,
            customClient = testClient,
            imagesDirProvider = { tempFolder.root }
        )

        val targetUrl = "https://lh3.googleusercontent.com/gg-dl/mock_image_token"
        val file = repository.downloadImage(
            url = targetUrl,
            modelName = "gemini-flash"
        )

        assertTrue(file.exists())
        assertEquals(fakeImageData.size.toLong(), file.length())
        assertTrue(file.name.endsWith(".png"))

        val recorded = recordedRequest!!
        assertEquals("image", recorded.header("Sec-Fetch-Dest"))
        assertEquals("cross-site", recorded.header("Sec-Fetch-Site"))
        assertTrue(recorded.header("User-Agent")!!.contains("Mozilla"))
        assertEquals("https://lh3.googleusercontent.com/gg-dl/mock_image_token=s0-d", recorded.url.toString())
    }

    @Test
    fun testHttpDownload_DetectsJpegMagicBytes_SavesAsJpg() = kotlinx.coroutines.runBlocking {
        val fakeJpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46)
        val mockInterceptor = Interceptor { chain ->
            val req = chain.request()
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(fakeJpegData.toResponseBody("application/octet-stream".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = ImageRepositoryImpl(
            context = stubContext,
            customClient = testClient,
            imagesDirProvider = { tempFolder.root }
        )

        val targetUrl = "https://lh3.googleusercontent.com/gg-dl/mock_jpeg_token"
        val file = repository.downloadImage(
            url = targetUrl,
            modelName = "gemini-3-pro"
        )

        assertTrue(file.exists())
        assertEquals(fakeJpegData.size.toLong(), file.length())
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun testHttpDownload_WithExportService_DownloadsHighResUrl() = kotlinx.coroutines.runBlocking {
        val fakeJpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46)
        var requestedUrl: String? = null

        val mockInterceptor = Interceptor { chain ->
            val req = chain.request()
            requestedUrl = req.url.toString()
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(fakeJpegData.toResponseBody("image/jpeg".toMediaType()))
                .build()
        }

        val testClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val mockExportService = object : com.geminiflow.app.domain.repository.ImageExportService {
            override suspend fun exportHighResolutionImageUrl(
                metadata: com.geminiflow.app.domain.model.export.ImageExportMetadata,
                tokens: com.geminiflow.app.domain.model.auth.GeminiTokens,
                cookies: Map<String, String>
            ): Result<String> {
                return Result.success("https://lh3.googleusercontent.com/gg-dl/high_res_2816x1536=s0")
            }
        }

        val repository = ImageRepositoryImpl(
            context = stubContext,
            customClient = testClient,
            imagesDirProvider = { tempFolder.root },
            exportService = mockExportService
        )

        val targetUrl = "https://lh3.googleusercontent.com/gg-dl/preview_1408x768"
        val metadata = com.geminiflow.app.domain.model.export.ImageExportMetadata(
            conversationId = "c_1",
            responseId = "r_2",
            choiceId = "rc_3",
            imageId = "im_1",
            imageBlockJson = "[]",
            previewUrl = targetUrl
        )

        val file = repository.downloadExportedImage(
            metadata = metadata,
            modelName = "gemini-3-pro",
            tokens = com.geminiflow.app.domain.model.auth.GeminiTokens("at_token", "sid"),
            cookies = mapOf("c" to "v")
        )

        assertTrue(file.exists())
        assertEquals("https://lh3.googleusercontent.com/gg-dl/high_res_2816x1536=s0", requestedUrl)
    }

    @Test
    fun testHttpDownload_ExportFails_ThrowsExceptionExplicitlyWithoutFallback() = kotlinx.coroutines.runBlocking {
        val mockExportService = object : com.geminiflow.app.domain.repository.ImageExportService {
            override suspend fun exportHighResolutionImageUrl(
                metadata: com.geminiflow.app.domain.model.export.ImageExportMetadata,
                tokens: com.geminiflow.app.domain.model.auth.GeminiTokens,
                cookies: Map<String, String>
            ): Result<String> {
                return Result.failure(java.io.IOException("Google c8o8Fe 導出錯誤代碼 [13]"))
            }
        }

        val repository = ImageRepositoryImpl(
            context = stubContext,
            imagesDirProvider = { tempFolder.root },
            exportService = mockExportService
        )

        val targetUrl = "https://lh3.googleusercontent.com/gg-dl/preview_1408x768"
        val metadata = com.geminiflow.app.domain.model.export.ImageExportMetadata(
            conversationId = "c_1",
            responseId = "r_2",
            choiceId = "rc_3",
            imageId = "im_1",
            imageBlockJson = "[]",
            previewUrl = targetUrl
        )

        try {
            repository.downloadExportedImage(
                metadata = metadata,
                modelName = "gemini-3-pro",
                tokens = com.geminiflow.app.domain.model.auth.GeminiTokens("at_token", "sid"),
                cookies = mapOf("c" to "v")
            )
            org.junit.Assert.fail("應當顯性拋出 ImageDownloadException")
        } catch (e: Exception) {
            assertTrue(e is com.geminiflow.app.domain.model.common.ImageDownloadException)
            assertTrue(e.message?.contains("c8o8Fe") == true)
        }
    }

    @Test
    fun testGoogleCdnUrlNormalizer_RewritesToS0D() {
        val normalizer = com.geminiflow.app.data.network.GoogleCdnUrlNormalizer()

        // 1. 帶有預設縮圖參數 =s512 應被替換為 =s0-d
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/token=s0-d",
            normalizer.normalize("https://lh3.googleusercontent.com/gg-dl/token=s512")
        )

        // 2. 帶有寬高裁切參數 =w1024-h768 應被替換為 =s0-d
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/token=s0-d",
            normalizer.normalize("https://lh3.googleusercontent.com/gg-dl/token=w1024-h768")
        )

        // 3. 無尾端參數的 googleusercontent 網址應自動追加 =s0-d
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/token=s0-d",
            normalizer.normalize("https://lh3.googleusercontent.com/gg-dl/token")
        )

        // 4. 帶有 query 參數 (?authuser=0) 應保留 query 並將 base 替換為 =s0-d
        assertEquals(
            "https://lh3.googleusercontent.com/gg-dl/token=s0-d?authuser=0",
            normalizer.normalize("https://lh3.googleusercontent.com/gg-dl/token=s512?authuser=0")
        )

        // 5. 非 Google 網址應保持原樣
        assertEquals(
            "https://example.com/image.png",
            normalizer.normalize("https://example.com/image.png")
        )

        // 6. Data URI 應保持原樣
        val dataUri = "data:image/png;base64,iVBORw0KGgo="
        assertEquals(dataUri, normalizer.normalize(dataUri))
    }

    @Test
    fun testGetAllCachedImages_AndSingleDeletion() = kotlinx.coroutines.runBlocking {
        val repository = ImageRepositoryImpl(
            context = stubContext,
            imagesDirProvider = { tempFolder.root }
        )

        val file1 = File(tempFolder.root, "20260901_120000_gemini-pro_generated.png")
        file1.writeBytes(byteArrayOf(1, 2, 3, 4))
        file1.setLastModified(1756700000000L)

        val file2 = File(tempFolder.root, "20260915_150000_imagen-3_generated.png")
        file2.writeBytes(byteArrayOf(5, 6, 7))
        file2.setLastModified(1757900000000L)

        val list = repository.getAllCachedImages()
        assertEquals(2, list.size)
        assertEquals("20260915_150000_imagen-3_generated.png", list[0].filename)
        assertEquals("imagen-3", list[0].modelName)
        assertEquals(3L, list[0].sizeBytes)

        val deleted = repository.deleteCachedImage(file1.name)
        assertTrue(deleted)

        val remaining = repository.getAllCachedImages()
        assertEquals(1, remaining.size)
        assertEquals(file2.name, remaining[0].filename)

        val (count, bytes) = repository.getCacheStats()
        assertEquals(1, count)
        assertEquals(3L, bytes)
    }
}
