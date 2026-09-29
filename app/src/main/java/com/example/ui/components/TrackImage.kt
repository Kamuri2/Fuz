package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import com.example.data.AlbumArtExtractor
import com.example.data.ArtworkExtractor
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-definition Player Album Art:
 * 1. Lee el artwork embebido a resolución original (NO usa MediaStore que downscalea a 512px).
 * 2. Size.ORIGINAL en Coil (sin downsampling).
 * 3. FilterQuality.High (escalado bicúbico cuando la fuente se muestra a gran tamaño en el reproductor).
 * 4. Caché persistente en disco (album_art_hd) para 0 I/O recurrente.
 */
@Composable
fun PlayerAlbumArt(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val extractor = remember { AlbumArtExtractor(context) }

    val artFile by produceState<File?>(initialValue = null, key1 = track.id) {
        value = withContext(Dispatchers.IO) {
            extractor.getHighResArt(track)
        }
    }

    val model: Any? = artFile ?: track.albumArtUri ?: if (track.contentUri != Uri.EMPTY) track.contentUri else null

    if (model != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .size(CoilSize.ORIGINAL)
                .crossfade(true)
                .build(),
            contentDescription = "Portada",
            contentScale = contentScale,
            filterQuality = FilterQuality.High,
            modifier = modifier,
            error = {
                FallbackOrExtractImage(track = track, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
            },
            loading = {
                FallbackMusicIcon(modifier = Modifier.fillMaxSize())
            }
        )
    } else {
        FallbackOrExtractImage(track = track, modifier = modifier, contentScale = contentScale)
    }
}

/**
 * Versión de PlayerAlbumArt para archivo directo File
 */
@Composable
fun PlayerAlbumArt(
    audioFile: File,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val extractor = remember { AlbumArtExtractor(context) }

    val artFile by produceState<File?>(initialValue = null, key1 = audioFile.absolutePath) {
        value = withContext(Dispatchers.IO) {
            extractor.getHighResArt(audioFile)
        }
    }

    val model: Any = artFile ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .size(CoilSize.ORIGINAL)
            .crossfade(true)
            .build(),
        contentDescription = "Portada",
        contentScale = contentScale,
        filterQuality = FilterQuality.High,
        modifier = modifier,
        error = {
            FallbackMusicIcon(modifier = Modifier.fillMaxSize())
        },
        loading = {
            FallbackMusicIcon(modifier = Modifier.fillMaxSize())
        }
    )
}

/**
 * Standard Coil Album Art component with crossfade and view-size downsampling.
 */
@Composable
fun AlbumArt(
    uri: Uri,
    size: Dp = 300.dp,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .size(CoilSize.ORIGINAL)
            .crossfade(true)
            .build(),
        contentDescription = "Portada",
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.High,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
    )
}

/**
 * Complete TrackImage combining:
 * 1. MediaStore album art URI with Coil async loading and crossfade
 * 2. Downsampled off-main-thread extraction via MediaMetadataRetriever/JAudioTagger
 * 3. Two-level caching (LRU Memory + Disk cache)
 * 4. Graceful placeholder fallback
 */
@Composable
fun TrackImage(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    if (track.albumArtUri != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(track.albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = track.title,
            contentScale = contentScale,
            modifier = modifier,
            error = {
                FallbackOrExtractImage(track = track, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
            },
            loading = {
                FallbackMusicIcon(modifier = Modifier.fillMaxSize())
            }
        )
    } else {
        FallbackOrExtractImage(track = track, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
private fun FallbackOrExtractImage(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = ArtworkExtractor.getCachedBitmap(track), key1 = track.id) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                ArtworkExtractor.loadArtworkBitmap(context, track, 600)
            }
        }
    }

    if (bitmap != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(bitmap)
                .crossfade(true)
                .build(),
            contentDescription = track.title,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        FallbackMusicIcon(modifier = modifier)
    }
}

@Composable
fun FallbackMusicIcon(modifier: Modifier = Modifier) {
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
