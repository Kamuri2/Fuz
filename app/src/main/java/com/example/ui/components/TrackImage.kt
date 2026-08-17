package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.example.model.Track
import java.io.File
import org.jaudiotagger.audio.AudioFileIO
import android.util.LruCache

object ArtworkCache {
    // Calculate max memory in KB
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/6th of the available memory for this memory cache.
    private val cacheSize = maxMemory / 6

    val cache = object : LruCache<String, android.graphics.Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: android.graphics.Bitmap): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return bitmap.byteCount / 1024
        }
    }
}

@Composable
fun TrackImage(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(track.id) { mutableStateOf<android.graphics.Bitmap?>(ArtworkCache.cache.get(track.path)) }
    var useFallback by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        if (bitmap == null && !useFallback) {
            withContext(Dispatchers.IO) {
                try {
                    if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                        val file = File(track.path)
                        if (file.exists()) {
                            val audioFile = AudioFileIO.read(file)
                            val rawBytes = audioFile.tag?.firstArtwork?.binaryData
                            if (rawBytes != null) {
                                val decoded: android.graphics.Bitmap?
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                                    decoded = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                        var sampleSize = 1
                                        if (info.size.height > 800 || info.size.width > 800) {
                                            var halfHeight = info.size.height / 2
                                            var halfWidth = info.size.width / 2
                                            while (halfHeight / sampleSize >= 800 && halfWidth / sampleSize >= 800) {
                                                sampleSize *= 2
                                            }
                                        }
                                        decoder.setTargetSampleSize(sampleSize)
                                        // Avoid hardware bitmaps in cache to prevent potential issues
                                        // decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                                    }
                                } else {
                                    val options = BitmapFactory.Options().apply {
                                        inJustDecodeBounds = true
                                    }
                                    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    
                                    var inSampleSize = 1
                                    if (options.outHeight > 800 || options.outWidth > 800) {
                                        val halfHeight = options.outHeight / 2
                                        val halfWidth = options.outWidth / 2
                                        while (halfHeight / inSampleSize >= 800 && halfWidth / inSampleSize >= 800) {
                                            inSampleSize *= 2
                                        }
                                    }

                                    val finalOptions = BitmapFactory.Options().apply {
                                        this.inSampleSize = inSampleSize
                                        inPreferredConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    android.graphics.Bitmap.Config.HARDWARE
                                } else {
                                    android.graphics.Bitmap.Config.ARGB_8888
                                }
                                    }
                                    decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)
                                }
                                if (decoded != null) {
                                    ArtworkCache.cache.put(track.path, decoded)
                                    bitmap = decoded
                                    return@withContext
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
                
                // If it fails or has no embedded art, fallback to Coil
                useFallback = true
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = track.title,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (useFallback && track.albumArtUri != null) {
        AsyncImage(
            model = track.albumArtUri,
            contentDescription = track.title,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
