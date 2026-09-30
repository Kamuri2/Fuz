package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.LruCache
import com.example.model.Track
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ArtworkExtractor {

    private const val TAG = "ArtworkExtractor"

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 6
    val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val negativeCache = java.util.Collections.synchronizedSet(HashSet<String>())

    private fun getCacheKey(track: Track): String {
        return if (track.path.isNotBlank()) track.path else track.contentUri.toString()
    }

    fun saveArtworkToInternalCache(context: Context, track: Track): Uri? {
        try {
            val key = getCacheKey(track)
            if (key.isNotBlank() && negativeCache.contains(key)) return null

            val bytes = extractArtworkBytes(context, track)
            if (bytes == null) {
                if (key.isNotBlank()) negativeCache.add(key)
                return null
            }
            val bitmap = decodeSampledBitmapFromByteArray(bytes, 400) ?: return null
            
            val cacheDir = File(context.cacheDir, "thumbnails")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, "thumb_${track.id}.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            fos.close()
            
            return Uri.fromFile(file)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to save artwork to cache: ${e.message}")
            return null
        }
    }

    fun getCachedBitmap(track: Track): Bitmap? {
        val key = getCacheKey(track)
        if (key.isBlank() || negativeCache.contains(key)) return null
        return cache.get(key)
    }

    fun loadArtworkBitmap(context: Context, track: Track, targetDim: Int = 600): Bitmap? {
        val key = getCacheKey(track)
        if (key.isNotBlank()) {
            if (negativeCache.contains(key)) return null
            cache.get(key)?.let { return it }
        }

        // 1. Check pre-extracted HD disk cache (0 ms)
        try {
            val hdFile = AlbumArtExtractor(context).getExistingArtFile(track)
            if (hdFile != null && hdFile.exists() && hdFile.length() > 0) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(hdFile.absolutePath, opts)
                var sampleSize = 1
                while (opts.outWidth / (sampleSize * 2) >= targetDim &&
                       opts.outHeight / (sampleSize * 2) >= targetDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(hdFile.absolutePath, decodeOpts)
                if (bitmap != null) {
                    if (key.isNotBlank()) cache.put(key, bitmap)
                    return bitmap
                }
            }
        } catch (_: Exception) {}

        // 2. Try JAudioTagger / Vorbis / Sidecar extraction directly (pure Kotlin/Java, no JNI errors)
        val rawBytes = extractArtworkBytes(context, track)
        if (rawBytes != null && rawBytes.isNotEmpty()) {
            val bitmap = decodeSampledBitmapFromByteArray(rawBytes, targetDim)
            if (bitmap != null) {
                if (key.isNotBlank()) cache.put(key, bitmap)
                return bitmap
            }
        }

        // 3. Fallback to MediaMetadataRetriever once if JAudioTagger could not read
        if (track.contentUri != Uri.EMPTY) {
            getAlbumArtDownsampled(context, track.contentUri, targetDim)?.let { bitmap ->
                if (key.isNotBlank()) cache.put(key, bitmap)
                return bitmap
            }
        } else if (track.path.isNotBlank()) {
            getAlbumArtDownsampled(track.path, targetDim)?.let { bitmap ->
                if (key.isNotBlank()) cache.put(key, bitmap)
                return bitmap
            }
        }

        if (key.isNotBlank()) {
            negativeCache.add(key)
        }
        return null
    }

    /**
     * Efficient downsampled embedded artwork extraction via MediaMetadataRetriever
     * avoids allocating large 3000x3000px bitmaps in memory.
     */
    fun getAlbumArtDownsampled(
        context: Context,
        uri: Uri,
        targetSize: Int = 600
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

            var sampleSize = 1
            while (opts.outWidth / (sampleSize * 2) >= targetSize &&
                   opts.outHeight / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    fun getAlbumArtDownsampled(
        path: String,
        targetSize: Int = 600
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val bytes = retriever.embeddedPicture ?: return null

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

            var sampleSize = 1
            while (opts.outWidth / (sampleSize * 2) >= targetSize &&
                   opts.outHeight / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    fun extractArtworkBytes(context: Context, track: Track): ByteArray? {
        // 1. Try MediaStore album art URI first if available (pre-indexed, avoids reading entire audio file)
        if (track.albumArtUri != null) {
            try {
                context.contentResolver.openInputStream(track.albumArtUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) {
                        return bytes
                    }
                }
            } catch (e: Exception) {
                // MediaStore albumart entry not available on disk
            }
        }

        var fileTagChecked = false

        // 2. Try JAudioTagger (pure Java, handles MP3, FLAC, M4A, OGG, Opus, WAV without native MMR)
        if (track.path.isNotBlank()) {
            try {
                val file = File(track.path)
                if (file.exists() && file.canRead()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val tag = audioFile.tag
                    if (tag != null) {
                        fileTagChecked = true
                        tag.firstArtwork?.binaryData?.takeIf { it.isNotEmpty() }?.let {
                            return it
                        }
                        
                        // Check vorbis comments directly if available via JAudioTagger
                        val picBase64 = tag.getFirst("METADATA_BLOCK_PICTURE")
                        if (!picBase64.isNullOrBlank()) {
                            val decoded = decodeVorbisPictureBlock(picBase64)
                            if (decoded != null && decoded.isNotEmpty()) {
                                return decoded
                            }
                        }
                        val coverBase64 = tag.getFirst("COVERART") ?: tag.getFirst("COVER_ART")
                        if (!coverBase64.isNullOrBlank()) {
                            try {
                                val bytes = Base64.decode(coverBase64.trim(), Base64.DEFAULT)
                                if (bytes.isNotEmpty()) return bytes
                            } catch (e: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "JAudioTagger failed for artwork: ${e.message}")
            }
        }

        // 3. Check sidecar cover images in folder (cover.jpg, folder.jpg, album.jpg)
        if (track.path.isNotBlank()) {
            try {
                val audioFile = File(track.path)
                val parent = audioFile.parentFile
                if (parent != null && parent.exists() && parent.isDirectory) {
                    val candidateNames = listOf(
                        "cover.jpg", "cover.png", "cover.webp", "cover.jpeg",
                        "folder.jpg", "folder.png", "folder.webp", "folder.jpeg",
                        "album.jpg", "album.png", "album.jpeg",
                        "${audioFile.nameWithoutExtension}.jpg",
                        "${audioFile.nameWithoutExtension}.png"
                    )
                    for (name in candidateNames) {
                        val sidecar = File(parent, name)
                        if (sidecar.exists() && sidecar.canRead()) {
                            val bytes = sidecar.readBytes()
                            if (bytes.isNotEmpty()) {
                                return bytes
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        // 4. Specialized direct extractor for Opus, OGG, and Vorbis comments if file tag wasn't checked
        if (!fileTagChecked) {
            val directOpusBytes = extractFromVorbisOrOpus(context, track)
            if (directOpusBytes != null && directOpusBytes.isNotEmpty()) {
                return directOpusBytes
            }
        }

        return null
    }

    /**
     * Reads Opus or Ogg Vorbis comments directly from file or ContentResolver stream.
     * Scans for METADATA_BLOCK_PICTURE or COVERART base64 data.
     */
    private fun extractFromVorbisOrOpus(context: Context, track: Track): ByteArray? {
        var inputStream: InputStream? = null
        try {
            if (track.path.isNotBlank()) {
                val f = File(track.path)
                if (f.exists() && f.canRead()) {
                    inputStream = f.inputStream()
                }
            }
            if (inputStream == null && track.contentUri != Uri.EMPTY) {
                inputStream = context.contentResolver.openInputStream(track.contentUri)
            }
            if (inputStream == null) return null

            // Read the first 10MB to ensure we don't truncate high-res Opus cover arts
            val buffer = ByteArray(10 * 1024 * 1024)
            var totalRead = 0
            while (totalRead < buffer.size) {
                val read = inputStream.read(buffer, totalRead, buffer.size - totalRead)
                if (read <= 0) break
                totalRead += read
            }

            if (totalRead <= 0) return null

            // Search for METADATA_BLOCK_PICTURE= or COVERART= in buffer
            val metaPattern = "METADATA_BLOCK_PICTURE=".toByteArray(Charsets.US_ASCII)
            val coverPattern = "COVERART=".toByteArray(Charsets.US_ASCII)

            val metaIndex = indexOfPattern(buffer, totalRead, metaPattern)
            if (metaIndex != -1) {
                val start = metaIndex + metaPattern.size
                
                // Try to read Vorbis length prefix if available
                var length = -1
                if (metaIndex >= 4) {
                    val b0 = buffer[metaIndex - 4].toInt() and 0xFF
                    val b1 = buffer[metaIndex - 3].toInt() and 0xFF
                    val b2 = buffer[metaIndex - 2].toInt() and 0xFF
                    val b3 = buffer[metaIndex - 1].toInt() and 0xFF
                    length = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
                }
                
                val base64Str = if (length > metaPattern.size && metaIndex - 4 + 4 + length <= totalRead) {
                    String(buffer, start, length - metaPattern.size, Charsets.US_ASCII)
                } else {
                    extractBase64String(buffer, start, totalRead)
                }
                
                if (base64Str.isNotBlank()) {
                    val decoded = decodeVorbisPictureBlock(base64Str)
                    if (decoded != null && decoded.isNotEmpty()) {
                        return decoded
                    }
                }
            }

            val coverIndex = indexOfPattern(buffer, totalRead, coverPattern)
            if (coverIndex != -1) {
                val start = coverIndex + coverPattern.size
                val base64Str = extractBase64String(buffer, start, totalRead)
                if (base64Str.isNotBlank()) {
                    try {
                        val decoded = Base64.decode(base64Str, Base64.DEFAULT)
                        if (decoded.isNotEmpty()) {
                            return decoded
                        }
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "extractFromVorbisOrOpus error: ${e.message}")
        } finally {
            try { inputStream?.close() } catch (e: Exception) {}
        }
        return null
    }

    private fun indexOfPattern(data: ByteArray, limit: Int, pattern: ByteArray): Int {
        if (pattern.isEmpty() || limit < pattern.size) return -1
        val max = limit - pattern.size
        for (i in 0..max) {
            var found = true
            for (j in pattern.indices) {
                // Case insensitive for ASCII letters
                val b1 = data[i + j].toInt().toChar().uppercaseChar().code.toByte()
                val b2 = pattern[j].toInt().toChar().uppercaseChar().code.toByte()
                if (b1 != b2) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun extractBase64String(data: ByteArray, start: Int, limit: Int): String {
        var end = start
        while (end < limit) {
            val b = data[end]
            // Valid Base64 chars are A-Z, a-z, 0-9, +, /, =, and whitespace (CR/LF/Space)
            val isValidBase64 = (b >= 'A'.code.toByte() && b <= 'Z'.code.toByte()) ||
                    (b >= 'a'.code.toByte() && b <= 'z'.code.toByte()) ||
                    (b >= '0'.code.toByte() && b <= '9'.code.toByte()) ||
                    b == '+'.code.toByte() || b == '/'.code.toByte() || b == '='.code.toByte() ||
                    b == '\r'.code.toByte() || b == '\n'.code.toByte() || b == ' '.code.toByte()
            if (!isValidBase64) {
                break
            }
            end++
        }
        if (end <= start) return ""
        return String(data, start, end - start, Charsets.US_ASCII).replace("\r", "").replace("\n", "").replace(" ", "").trim()
    }

    private fun decodeVorbisPictureBlock(base64Str: String): ByteArray? {
        return try {
            val blockBytes = Base64.decode(base64Str.trim(), Base64.DEFAULT)
            parseFlacPictureBlock(blockBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFlacPictureBlock(blockBytes: ByteArray): ByteArray? {
        try {
            if (blockBytes.size < 32) return null
            val buffer = ByteBuffer.wrap(blockBytes)
            buffer.order(ByteOrder.BIG_ENDIAN)
            val picType = buffer.int
            val mimeLen = buffer.int
            if (mimeLen < 0 || mimeLen > buffer.remaining()) return null
            buffer.position(buffer.position() + mimeLen) // skip mime
            val descLen = buffer.int
            if (descLen < 0 || descLen > buffer.remaining()) return null
            buffer.position(buffer.position() + descLen) // skip desc
            if (buffer.remaining() < 16 + 4) return null
            buffer.position(buffer.position() + 16) // skip width(4), height(4), depth(4), colors(4)
            val picDataLen = buffer.int
            if (picDataLen <= 0 || picDataLen > buffer.remaining()) {
                return null // Avoid returning truncated array that causes broken images
            }
            val picData = ByteArray(picDataLen)
            buffer.get(picData)
            return picData
        } catch (e: Exception) {
            return null
        }
    }

    fun decodeSampledBitmapFromByteArray(data: ByteArray, targetDim: Int): Bitmap? {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, boundsOptions)

            var sampleSize = 1
            if (boundsOptions.outHeight > targetDim || boundsOptions.outWidth > targetDim) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while (halfHeight / sampleSize >= targetDim && halfWidth / sampleSize >= targetDim) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
        } catch (e: Exception) {
            Log.d(TAG, "decodeSampledBitmap error: ${e.message}")
            return null
        }
    }
}
