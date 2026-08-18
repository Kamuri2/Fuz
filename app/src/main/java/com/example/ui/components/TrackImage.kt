package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
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
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                            try {
                                mmr.setDataSource(track.path)
                            } catch (e: Exception) {
                                mmr.setDataSource(context, track.contentUri)
                            }
                        } else {
                            mmr.setDataSource(context, track.contentUri)
                        }
                        
                        var rawBytes = mmr.embeddedPicture
                        
                        // Si falla o es nulo (común en OGG/OPUS), intentamos con JAudioTagger
                        if (rawBytes == null) {
                            try {
                                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(java.io.File(track.path))
                                val tag = audioFile.tag
                                val artwork = tag?.firstArtwork
                                if (artwork != null) {
                                    rawBytes = artwork.binaryData
                                }
                            } catch (e: Exception) {
                                // Ignorar
                            }
                        }

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
                                }
                            } else {
                                val options = android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    
                                var inSampleSize = 1
                                if (options.outHeight > 800 || options.outWidth > 800) {
                                    val halfHeight = options.outHeight / 2
                                    val halfWidth = options.outWidth / 2
                                    while (halfHeight / inSampleSize >= 800 && halfWidth / inSampleSize >= 800) {
                                        inSampleSize *= 2
                                    }
                                }

                                val finalOptions = android.graphics.BitmapFactory.Options().apply {
                                    this.inSampleSize = inSampleSize
                                    inPreferredConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        android.graphics.Bitmap.Config.HARDWARE
                                    } else {
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    }
                                }
                                decoded = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)
                            }
                            if (decoded != null) {
                                ArtworkCache.cache.put(track.path, decoded)
                                bitmap = decoded
                                return@withContext
                            }
                        }
                    } finally {
                        mmr.release()
                    }
                } catch (e: Exception) {}
                
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
    } else if (useFallback) {
        Box(
            modifier = modifier.background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }
}
