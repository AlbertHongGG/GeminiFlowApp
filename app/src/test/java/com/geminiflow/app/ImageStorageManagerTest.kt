package com.geminiflow.app

import android.content.Context
import com.geminiflow.app.data.storage.ImageStorageManager
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

class ImageStorageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // Lightweight mock Context that satisfies ImageStorageManager without Mockito
    private val stubContext = object : android.content.ContextWrapper(null) {
        override fun getFilesDir(): File = tempFolder.root
    }

    @Test
    fun testBase64Decoding_WritesDirectlyToFile() = kotlinx.coroutines.runBlocking {
        val manager = ImageStorageManager(
            context = stubContext,
            imagesDirProvider = { tempFolder.root }
        )

        // 1x1 transparent PNG base64
        val base64Data = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        val file = manager.downloadImage(base64Data, "gemini-3-pro")

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.contains("gemini-3-pro_generated.png"))
    }

    @Test
    fun testHttpDownload_SendsBrowserHeadersAndAtomicSave() = kotlinx.coroutines.runBlocking {
        val fakeImageData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // PNG signature
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

        val manager = ImageStorageManager(
            context = stubContext,
            customClient = testClient,
            imagesDirProvider = { tempFolder.root }
        )

        val targetUrl = "https://lh3.googleusercontent.com/gg-dl/mock_image_token"
        val file = manager.downloadImage(targetUrl, "gemini-flash")

        assertTrue(file.exists())
        assertEquals(fakeImageData.size.toLong(), file.length())

        val recorded = recordedRequest!!
        assertEquals("image", recorded.header("Sec-Fetch-Dest"))
        assertEquals("cross-site", recorded.header("Sec-Fetch-Site"))
        assertTrue(recorded.header("User-Agent")!!.contains("Mozilla"))
    }
}
