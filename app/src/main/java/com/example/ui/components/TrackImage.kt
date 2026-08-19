package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.util.LruCache
import android.util.Log
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 6
    val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
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
    var bitmap by remember(track.id) { mutableStateOf<Bitmap?>(ArtworkCache.cache.get(track.path)) }
    var useFallback by remember(track.id) { mutableStateOf(false) }

        LaunchedEffect(track.id) {
        if (bitmap == null && !useFallback) {
            withContext(Dispatchers.IO) {
                try {
                    var rawBytes: ByteArray? = null
                    
                    if (track.path.isNotBlank()) {
                        try {
                            val file = java.io.File(track.path)
                            if (file.exists() && file.canRead()) {
                                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                                rawBytes = audioFile.tag?.firstArtwork?.binaryData
                            }
                        } catch (e: Exception) {
                            Log.d("TrackImage", "JAudioTagger failed for artwork: ${e.message}")
                        }
                    }
                    
                    if (rawBytes == null || rawBytes.isEmpty()) {
                        try {
                            val mmr = MediaMetadataRetriever()
                            mmr.setDataSource(context, track.contentUri)
                            rawBytes = mmr.embeddedPicture
                            mmr.release()
                        } catch (e: Exception) {}
                    }
                    
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val boundsOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
                        
                        var sampleSize = 1
                        val targetDim = 600
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
                        
                        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                        if (decoded != null) {
                            ArtworkCache.cache.put(track.path, decoded)
                            bitmap = decoded
                            return@withContext
                        }
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
