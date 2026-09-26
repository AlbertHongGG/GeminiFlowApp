package com.geminiflow.app.domain.model.cache

/**
 * 圖片格式強型別列舉。
 * 負責以二進位魔術位元組 (Magic Bytes) 及 MIME Type 為依據進行精確格式識別，
 * 徹底杜絕信任外部或不可靠之文字副檔名。
 */
enum class ImageFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    JPEG("jpg", "image/jpeg", "JPEG"),
    PNG("png", "image/png", "PNG"),
    WEBP("webp", "image/webp", "WebP"),
    GIF("gif", "image/gif", "GIF"),
    UNKNOWN("bin", "application/octet-stream", "未知格式");

    companion object {
        /**
         * 依據二進位串流開頭的魔術位元組 (Magic Bytes) 判定真實圖片格式。
         */
        fun fromHeaderBytes(bytes: ByteArray): ImageFormat {
            if (bytes.size < 3) return UNKNOWN

            // JPEG: FF D8 FF
            if ((bytes[0].toInt() and 0xFF) == 0xFF &&
                (bytes[1].toInt() and 0xFF) == 0xD8 &&
                (bytes[2].toInt() and 0xFF) == 0xFF
            ) {
                return JPEG
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (bytes.size >= 8 &&
                (bytes[0].toInt() and 0xFF) == 0x89 &&
                (bytes[1].toInt() and 0xFF) == 0x50 &&
                (bytes[2].toInt() and 0xFF) == 0x4E &&
                (bytes[3].toInt() and 0xFF) == 0x47 &&
                (bytes[4].toInt() and 0xFF) == 0x0D &&
                (bytes[5].toInt() and 0xFF) == 0x0A &&
                (bytes[6].toInt() and 0xFF) == 0x1A &&
                (bytes[7].toInt() and 0xFF) == 0x0A
            ) {
                return PNG
            }

            // WEBP: RIFF (bytes 0..3) ... WEBP (bytes 8..11)
            if (bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            ) {
                return WEBP
            }

            // GIF: GIF87a 或 GIF89a
            if (bytes.size >= 6 &&
                bytes[0] == 'G'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == '8'.code.toByte() &&
                (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
                bytes[5] == 'a'.code.toByte()
            ) {
                return GIF
            }

            return UNKNOWN
        }

        /**
         * 依據 HTTP Content-Type 或 BitmapFactory.Options.outMimeType 判定圖片格式。
         */
        fun fromMimeType(mimeType: String?): ImageFormat {
            val normalized = mimeType?.lowercase()?.trim() ?: return UNKNOWN
            return when {
                normalized.contains("jpeg") || normalized.contains("jpg") -> JPEG
                normalized.contains("png") -> PNG
                normalized.contains("webp") -> WEBP
                normalized.contains("gif") -> GIF
                else -> UNKNOWN
            }
        }
    }
}
