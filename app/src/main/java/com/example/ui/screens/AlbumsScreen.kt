package com.example.ui.screens


import android.graphics.BitmapFactory

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Album

import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.Shuffle

import androidx.compose.material3.*

import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.onSizeChanged

import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.example.model.Track

import com.example.ui.theme.GlassTextMuted

import com.example.ui.theme.GlassTextSecondary

@Composable
fun AlbumsScreen(
    settings: com.example.model.AppSettings,
    tracks: List<Track>,
    onPlayAlbum: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumGroups = remember(tracks) {
        tracks.groupBy { it.album }
    }
    
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedAlbum != null) {
        selectedAlbum = null
    }

    if (selectedAlbum != null) {
        val albumTracks = (albumGroups[selectedAlbum] ?: emptyList()).sortedWith(
            compareBy(
                { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                { it.title.lowercase() }
            )
        )
        AlbumDetailScreen(
            albumName = selectedAlbum!!,
            tracks = albumTracks,
            onBack = { selectedAlbum = null },
            onPlayTrack = { index -> onPlayAlbum(albumTracks, index) },
            onShuffleAll = { onPlayAlbum(albumTracks.shuffled(), 0) }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = com.example.ui.Translations.get(settings.appLanguage, "albums"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (albumGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(com.example.ui.Translations.get(settings.appLanguage, "no_albums"), color = GlassTextMuted)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albumGroups.keys.toList()) { albumName ->
                        val albumTracks = albumGroups[albumName] ?: emptyList()
                        val firstTrack = albumTracks.firstOrNull()

                        val artworkBitmap = remember(firstTrack?.albumArtBytes) {
                            firstTrack?.albumArtBytes?.let { bytes ->
                                try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } catch (e: Exception) { null }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAlbum = albumName },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF2B2B2B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        artworkBitmap != null -> Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        firstTrack?.albumArtUri != null -> AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        else -> Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = Color(0xFFFF5C00), modifier = Modifier.size(48.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(text = albumName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "${firstTrack?.artist ?: "Various"} • ${albumTracks.size} tracks", fontSize = 12.sp, color = GlassTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                
                // A-Z Alphabetical Scroll Bar
                var barHeight by remember { mutableStateOf(0f) }
                var currentDragLetter by remember { mutableStateOf<String?>(null) }
                val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                
                if (currentDragLetter != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 40.dp)
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5C00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentDragLetter!!, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .onSizeChanged { barHeight = it.height.toFloat() }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { currentDragLetter = null },
                                onDragCancel = { currentDragLetter = null }
                            ) { change, _ ->
                                val y = change.position.y
                                if (barHeight > 0) {
                                    val index = ((y / barHeight) * alphabet.length).toInt().coerceIn(0, alphabet.length - 1)
                                    val targetLetter = alphabet[index].toString()
                                    currentDragLetter = targetLetter
                                    val targetIndex = albumGroups.keys.indexOfFirst { it.uppercase().startsWith(targetLetter) }
                                    if (targetIndex >= 0) {
                                        coroutineScope.launch {
                                            gridState.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                            }
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    alphabet.forEach { letter ->
                        Text(
                            text = letter.toString(),
                            fontSize = 10.sp,
                            color = GlassTextMuted,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumName: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onShuffleAll: () -> Unit
) {
    val firstTrack = tracks.firstOrNull()
    val artworkBitmap = remember(firstTrack?.albumArtBytes) {
        firstTrack?.albumArtBytes?.let { bytes ->
            try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } catch (e: Exception) { null }
        }
    }
    
    val sortedTracks = tracks

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.Black)
            ) {
                if (artworkBitmap != null) {
                    Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (firstTrack?.albumArtUri != null) {
                    AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))
                
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Text(com.example.ui.Translations.get(com.example.model.AppSettings().appLanguage, "album"), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = GlassTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = albumName, fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${firstTrack?.artist ?: "Unknown Artist"} • ${tracks.size} songs • ${firstTrack?.year ?: "Unknown"}",
                        fontSize = 14.sp,
                        color = GlassTextSecondary
                    )
                }
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
        
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onPlayTrack(0) },
                        modifier = Modifier.size(56.dp).background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = onShuffleAll) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = GlassTextMuted, modifier = Modifier.size(28.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#", color = GlassTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Text("Title", color = GlassTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GlassTextMuted, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.DarkGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        itemsIndexed(sortedTracks) { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayTrack(index) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (track.trackNumber > 0) "${track.trackNumber}" else "${index + 1}",
                    color = GlassTextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = track.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = track.artist, color = GlassTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                Text(
                    text = String.format("%d:%02d", (track.durationMs / 60000), (track.durationMs % 60000) / 1000),
                    color = GlassTextMuted,
                    fontSize = 12.sp
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}
