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
        val file = repository.downloadImage(base64Data, "gemini-3-pro")

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
        val file = repository.downloadImage(targetUrl, "gemini-flash")

        assertTrue(file.exists())
        assertEquals(fakeImageData.size.toLong(), file.length())

        val recorded = recordedRequest!!
        assertEquals("image", recorded.header("Sec-Fetch-Dest"))
        assertEquals("cross-site", recorded.header("Sec-Fetch-Site"))
        assertTrue(recorded.header("User-Agent")!!.contains("Mozilla"))
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
