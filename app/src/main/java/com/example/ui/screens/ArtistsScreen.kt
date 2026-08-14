package com.example.ui.screens


import android.content.Intent

import android.graphics.BitmapFactory

import android.net.Uri

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.MusicNote

import androidx.compose.material.icons.filled.Person

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

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.example.model.Track

import com.example.ui.theme.GlassTextMuted

import com.example.ui.theme.GlassTextSecondary

import com.example.data.ArtistInfoFetcher

import com.example.data.ArtistInfo

@Composable
fun ArtistsScreen(
    settings: com.example.model.AppSettings,
    tracks: List<Track>,
    onPlayArtist: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val artistGroups = remember(tracks) {
        tracks.filter { it.artist.isNotBlank() && it.artist != "Unknown Artist" }
            .groupBy { it.artist }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }
    
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedArtist != null) {
        selectedArtist = null
    }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (selectedArtist != null) {
        val artistTracks = artistGroups[selectedArtist] ?: emptyList()
        ArtistDetailScreen(
            artistName = selectedArtist!!,
            tracks = artistTracks,
            onBack = { selectedArtist = null },
            onPlayTrack = { index -> onPlayArtist(artistTracks, index) },
            onShuffleAll = { onPlayArtist(artistTracks.shuffled(), 0) },
            onShowAbout = { showAboutDialog = true }
        )
        
        if (showAboutDialog) {
            ArtistAboutScreen(
                artistName = selectedArtist!!,
                onBack = { showAboutDialog = false }
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = com.example.ui.Translations.get(settings.appLanguage, "artists"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (artistGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(com.example.ui.Translations.get(settings.appLanguage, "no_artists"), color = GlassTextMuted)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 20.dp)
                ) {
                    items(artistGroups.keys.toList()) { artistName ->
                        var imageUrl by remember { mutableStateOf<String?>(null) }
                        
                        LaunchedEffect(artistName) {
                            imageUrl = com.example.data.ArtistImageRepository.getArtistImageUrl(artistName)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArtist = artistName },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                val trackFallback = tracks.firstOrNull { it.artist == artistName }?.albumArtUri
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (trackFallback != null) {
                                    AsyncImage(
                                        model = trackFallback,
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artistName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
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
                                    val targetIndex = artistGroups.keys.indexOfFirst { it.uppercase().startsWith(targetLetter) }
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
fun ArtistDetailScreen(
    artistName: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onShuffleAll: () -> Unit,
    onShowAbout: () -> Unit
) {
    var artistInfo by remember { mutableStateOf<ArtistInfo?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(artistName) {
        artistInfo = ArtistInfoFetcher.fetchArtistInfo(artistName)
    }

    val albumsCount = tracks.map { it.album }.distinct().size

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.Black)
            ) {
                val firstTrack = tracks.firstOrNull()
                val artworkBitmap = remember(firstTrack?.albumArtBytes) {
                    firstTrack?.albumArtBytes?.let { bytes ->
                        try { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } catch (e: Exception) { null }
                    }
                }
                val fallbackImage = firstTrack?.albumArtUri
                
                if (artistInfo?.imageUrl != null) {
                    AsyncImage(
                        model = artistInfo?.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (artworkBitmap != null) {
                    Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (fallbackImage != null) {
                    AsyncImage(
                        model = fallbackImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ARTIST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GlassTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artistName,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$albumsCount albums • ${tracks.size} songs",
                        fontSize = 14.sp,
                        color = GlassTextSecondary
                    )
                }
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
        
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                if (artistInfo?.bio != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable { onShowAbout() }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ABOUT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlassTextMuted, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artistInfo?.bio ?: "",
                                color = GlassTextSecondary,
                                fontSize = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/${artistName}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF114221), contentColor = Color(0xFF1DB954))
                    ) {
                        Text("Spotify", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${artistName}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421111), contentColor = Color(0xFFFF0000))
                    ) {
                        Text("YouTube", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.apple.com/search?term=${artistName}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331122), contentColor = Color(0xFFFA243C))
                    ) {
                        Text("Apple Music", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onShowAbout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("About the Artist", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                Text("All songs", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        itemsIndexed(tracks) { index, track ->
            val artworkBitmap = remember(track.albumArtBytes) {
                track.albumArtBytes?.let { bytes ->
                    try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } catch(e: Exception) { null }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayTrack(index) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    color = GlassTextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp)
                )
                
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                    if (artworkBitmap != null) {
                        Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (track.albumArtUri != null) {
                        AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = track.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = track.album, color = GlassTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = String.format("%d:%02d", (track.durationMs / 60000), (track.durationMs % 60000) / 1000),
                    color = GlassTextMuted,
                    fontSize = 12.sp
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ArtistAboutScreen(
    artistName: String,
    onBack: () -> Unit
) {
    var artistInfo by remember { mutableStateOf<ArtistInfo?>(null) }
    
    LaunchedEffect(artistName) {
        artistInfo = ArtistInfoFetcher.fetchArtistInfo(artistName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.Black)
        ) {
            if (artistInfo?.imageUrl != null) {
                AsyncImage(
                    model = artistInfo?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
            
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            
            Text(
                text = artistName,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            )
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "${artistInfo?.followers ?: "0"}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "FOLLOWERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlassTextMuted, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "${artistInfo?.listeners ?: "0"}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "MONTHLY LISTENERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlassTextMuted, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = artistInfo?.origin ?: "Unknown", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "ORIGIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlassTextMuted, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = artistInfo?.bio ?: "",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

