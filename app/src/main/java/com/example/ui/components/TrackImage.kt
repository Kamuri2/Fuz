package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.Track

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
            error = { FallbackMusicIcon(modifier = Modifier.fillMaxSize()) },
            loading = { FallbackMusicIcon(modifier = Modifier.fillMaxSize()) }
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
