package com.geminiflow.app

import com.geminiflow.app.domain.model.cache.ImageFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageFormatTest {

    @Test
    fun testDetectJpegFromMagicBytes() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        assertEquals(ImageFormat.JPEG, ImageFormat.fromHeaderBytes(bytes))
        assertEquals("jpg", ImageFormat.JPEG.extension)
        assertEquals("JPEG", ImageFormat.JPEG.displayName)
    }

    @Test
    fun testDetectPngFromMagicBytes() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals(ImageFormat.PNG, ImageFormat.fromHeaderBytes(bytes))
        assertEquals("png", ImageFormat.PNG.extension)
        assertEquals("PNG", ImageFormat.PNG.displayName)
    }

    @Test
    fun testDetectWebpFromMagicBytes() {
        val bytes = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
        )
        assertEquals(ImageFormat.WEBP, ImageFormat.fromHeaderBytes(bytes))
        assertEquals("webp", ImageFormat.WEBP.extension)
        assertEquals("WebP", ImageFormat.WEBP.displayName)
    }

    @Test
    fun testDetectGifFromMagicBytes() {
        val bytes = byteArrayOf('G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte())
        assertEquals(ImageFormat.GIF, ImageFormat.fromHeaderBytes(bytes))
        assertEquals("gif", ImageFormat.GIF.extension)
    }

    @Test
    fun testUnknownBytes() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertEquals(ImageFormat.UNKNOWN, ImageFormat.fromHeaderBytes(bytes))
    }

    @Test
    fun testFromMimeType() {
        assertEquals(ImageFormat.JPEG, ImageFormat.fromMimeType("image/jpeg"))
        assertEquals(ImageFormat.JPEG, ImageFormat.fromMimeType("image/jpg"))
        assertEquals(ImageFormat.PNG, ImageFormat.fromMimeType("image/png"))
        assertEquals(ImageFormat.WEBP, ImageFormat.fromMimeType("image/webp"))
        assertEquals(ImageFormat.GIF, ImageFormat.fromMimeType("image/gif"))
        assertEquals(ImageFormat.UNKNOWN, ImageFormat.fromMimeType("text/plain"))
        assertEquals(ImageFormat.UNKNOWN, ImageFormat.fromMimeType(null))
    }
}
