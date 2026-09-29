package com.example.ui.screens


import androidx.compose.material3.MaterialTheme
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

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.delay
import com.example.ui.components.TrackImage

import com.example.model.Track

import com.example.ui.theme.GlassTextMuted

import com.example.ui.theme.GlassTextSecondary
import androidx.compose.material.icons.filled.Mic

@Composable
fun AlbumsScreen(
    settings: com.example.model.AppSettings,
    tracks: List<Track>,
    initialAlbumName: String? = null,
    onPlayAlbum: (List<Track>, Int) -> Unit,
    onDismissOverlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val albumGroups = remember(tracks) {
        tracks.groupBy { it.album }.toSortedMap(compareBy { it.lowercase() })
    }
    
    var selectedAlbum by remember { mutableStateOf<String?>(initialAlbumName) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(initialAlbumName) { if(initialAlbumName != null) selectedAlbum = initialAlbumName }
    BackHandler(enabled = selectedAlbum != null || onDismissOverlay != null) {
        if (selectedAlbum != null) {
            if (onDismissOverlay != null) onDismissOverlay()
            else selectedAlbum = null
        } else {
            onDismissOverlay?.invoke()
        }
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
            onBack = { 
                if (onDismissOverlay != null) onDismissOverlay()
                else selectedAlbum = null 
            },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDismissOverlay != null) {
                    IconButton(onClick = onDismissOverlay) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "albums"),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar álbum...", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            val filteredAlbums = remember(albumGroups, searchQuery) {
                if (searchQuery.isBlank()) albumGroups.keys.toList()
                else albumGroups.keys.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            val coroutineScope = rememberCoroutineScope()
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            val alphabet = remember { listOf("#") + ('A'..'Z').map { it.toString() } }

            val letterIndices = remember(filteredAlbums) {
                val map = mutableMapOf<String, Int>()
                filteredAlbums.forEachIndexed { index, albumName ->
                    val trimmed = albumName.trim()
                    val firstChar = trimmed.firstOrNull()?.uppercaseChar()
                    val letterKey = if (firstChar != null && firstChar in 'A'..'Z') firstChar.toString() else "#"
                    if (!map.containsKey(letterKey)) {
                        map[letterKey] = index
                    }
                }
                map
            }

            var activeLetterBubble by remember { mutableStateOf<String?>(null) }

            fun jumpToLetter(letter: String) {
                activeLetterBubble = letter
                val targetIndex = if (letter == "#") {
                    letterIndices["#"] ?: 0
                } else {
                    letterIndices[letter] ?: run {
                        val targetChar = letter.first()
                        letterIndices.entries
                            .filter { it.key != "#" && it.key.first() >= targetChar }
                            .minByOrNull { it.key.first() }?.value ?: 0
                    }
                }
                coroutineScope.launch {
                    gridState.scrollToItem(targetIndex)
                    delay(900)
                    if (activeLetterBubble == letter) {
                        activeLetterBubble = null
                    }
                }
            }

            // Horizontal Alphabet Quick-Jump Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                items(alphabet) { letter ->
                    val hasAlbums = letterIndices.containsKey(letter)
                    val isSelected = activeLetterBubble == letter
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    hasAlbums -> Color.White.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .clickable { jumpToLetter(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            fontSize = 13.sp,
                            fontWeight = if (hasAlbums) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.Black
                                hasAlbums -> Color.White
                                else -> Color.White.copy(alpha = 0.35f)
                            }
                        )
                    }
                }
            }

            if (filteredAlbums.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (albumGroups.isEmpty()) com.example.ui.Translations.get(settings.appLanguage, "no_albums") else "No hay resultados", color = GlassTextMuted)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAlbums) { albumName ->
                            val albumTracks = albumGroups[albumName] ?: emptyList()
                            val firstTrack = albumTracks.firstOrNull()

                            val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
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
                                            firstTrack != null -> TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())
                                            else -> Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
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
                    }

                    // Floating Letter Bubble on Jump
                    if (activeLetterBubble != null) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeLetterBubble!!,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
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
    initialAlbumName: String? = null,
    onBack: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onShuffleAll: () -> Unit
) {
    val firstTrack = tracks.firstOrNull()
    val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
    
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
                } else if (firstTrack != null) {
                    TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.title, 
                            color = MaterialTheme.colorScheme.onBackground, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (track.lyrics.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Lyrics available",
                                tint = GlassTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
