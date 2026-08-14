package com.example.ui.screens

import android.graphics.BitmapFactory
import com.example.ui.components.LiquidGlassBackground
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LrcParser
import com.example.model.Track
import com.example.player.LoopMode
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    settings: com.example.model.AppSettings,
    currentTrack: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    loopMode: LoopMode,
    isFavorite: Boolean,
    isDisliked: Boolean,
    playlistsMap: Map<String, List<Track>>,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onLoopCycle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDislikeToggle: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
var showLyricsMode by remember { mutableStateOf(false) }
    
    // Track ID to detect automatic changes
    val trackId = currentTrack?.id ?: -1L
    var oldTrackId by remember { mutableStateOf(-1L) }
    // 1 for forward (slide left), -1 for backward (slide right)
    var slideDirection by remember { mutableStateOf(1) }
    
    LaunchedEffect(trackId) {
        if (oldTrackId != -1L && oldTrackId != trackId) {
            // Assume forward if not explicitly set by buttons
            slideDirection = 1 
        }
        oldTrackId = trackId
    }
    var showAddToPlaylistModal by remember { mutableStateOf(false) }

    val parsedLyrics = remember(currentTrack?.lyrics) {
        LrcParser.parse(currentTrack?.lyrics ?: "")
    }

    var artworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Limpia la portada anterior si cambia la canción para evitar parpadeos de la canción pasada
    LaunchedEffect(currentTrack?.path) {
        artworkBitmap = null
    }

    // Procesa la portada asíncronamente en calidad máxima SOLO si está en reproducción actual
    LaunchedEffect(currentTrack?.path, isPlaying) {
        if (isPlaying && currentTrack?.albumArtBytes != null && artworkBitmap == null) {
            withContext(Dispatchers.IO) { // Hilo secundario para no congelar la UI
                try {
                    val bytes = currentTrack.albumArtBytes
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 // Máxima calidad de color (32 bits)
                        inDither = false // Desactiva el difuminado para colores más puros
                    }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    artworkBitmap = bitmap?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val formatTag = remember(currentTrack) {
        if (currentTrack == null) "FLAC  988 KBPS  44.1 KHZ"
        else {
            val extension = when {
                currentTrack.path.endsWith(".flac", true) -> "FLAC"
                currentTrack.path.endsWith(".mp3", true) -> "MP3"
                currentTrack.path.endsWith(".m4a", true) || currentTrack.path.endsWith(".aac", true) -> "AAC"
                currentTrack.path.endsWith(".wav", true) -> "WAV"
                else -> "FLAC"
            }
            "$extension  ${currentTrack.bitrate.uppercase()}  ${currentTrack.sampleRate.uppercase()}"
        }
    }

    LiquidGlassBackground(isPlaying = isPlaying, currentTrack = currentTrack, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount > 35f) {
                            onBack()
                        }
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header with Swipe Down Pill Handle & Back Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onBack() }
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Deslizar para cerrar",
                        tint = GlassTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = com.example.ui.Translations.get(settings.appLanguage, "playing_now"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextMuted,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))


AnimatedContent(
                targetState = currentTrack,
                transitionSpec = {
                    if (slideDirection == 1) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "Track Transition",
                modifier = Modifier.weight(1f)
            ) { targetTrack ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                    // Center Area: Enlarged Square Album Art OR Lyrics Display
                    if (!showLyricsMode) {
                    // Larger Prominent Album Artwork Cover
                    Box(
                    modifier = Modifier
                    .padding(top = 40.dp)
                    .fillMaxWidth(0.98f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1AFFFFFF)),
                    contentAlignment = Alignment.Center
                    ) {
                    when {
                    artworkBitmap != null -> {
                    Image(
                    bitmap = artworkBitmap!!,
                    contentDescription = targetTrack?.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                    )
                    }
                    targetTrack?.albumArtUri != null -> {
                    AsyncImage(
                    model = targetTrack.albumArtUri,
                    contentDescription = targetTrack?.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                    )
                    }
                    else -> {
                    Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(96.dp)
                    )
                    }
                    }
                    }
                    } else {
                    // Synced Lyrics Display
                    Box(
                    modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                    ) {

                    if (parsedLyrics.isEmpty()) {
                    if (targetTrack?.lyrics?.isNotBlank() == true) {
                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                    text = targetTrack.lyrics,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                    )
                    }
                    } else {
                    Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "lyrics_not_available"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                    )
                    }
                    } else {

                    val activeIndex = remember(currentPositionMs, parsedLyrics) {
                    LrcParser.getCurrentLineIndex(parsedLyrics, currentPositionMs)
                    }
                    val listState = rememberLazyListState()

                    LaunchedEffect(activeIndex) {
                    if (activeIndex >= 0) {
                    listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                    }
                    }

                    LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    itemsIndexed(items = parsedLyrics) { idx, line ->
                    val isActive = idx == activeIndex
                    Text(
                    text = line.text,
                    fontSize = if (isActive) 22.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    )
                    }
                    }
                    }
                    }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Title and Artist Centered
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                    text = targetTrack?.title ?: "Trapped",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                    text = targetTrack?.artist ?: "2Pac",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlassTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                    )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Icons Row
                    Row(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Heart / Favorite
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("player_favorite_btn")) {
                    Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF5C00) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                    )
                    }

                    // Dislike / Thumbs Down
                    IconButton(onClick = onDislikeToggle) {
                    Icon(
                    imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                    contentDescription = "Dislike",
                    tint = if (isDisliked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                    )
                    }

                    // Plus / Add to Playlist
                    IconButton(onClick = { showAddToPlaylistModal = true }) {
                    Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to playlist",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                    )
                    }

                    // Lyrics (Mic Icon)
                    Box(
                    modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (showLyricsMode) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable { showLyricsMode = !showLyricsMode },
                    contentAlignment = Alignment.Center
                    ) {
                    Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Lyrics",
                    tint = if (showLyricsMode) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                    )
                    }

                    // Queue Icon
                    IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                    Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = "Queue",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                    )
                    }
                    }

                    Spacer(modifier = Modifier.height(12.dp))


                }
            }
            
            // Progress Bar & Duration Labels
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatDuration(currentPositionMs), fontSize = 11.sp, color = GlassTextMuted)
                    Text(text = formatDuration(durationMs), fontSize = 11.sp, color = GlassTextMuted)
                }

                Slider(
                    value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                    onValueChange = { fraction -> onSeek((fraction * durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffleToggle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFFFF5C00) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = { slideDirection = -1; onPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { onPlayPauseToggle() }
                        .testTag("player_main_play_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(onClick = { slideDirection = 1; onNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(onClick = onLoopCycle) {
                    Icon(
                        imageVector = if (loopMode == LoopMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (loopMode != LoopMode.OFF) Color(0xFFFF5C00) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Quality Pill Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = formatTag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Modal to add song to custom playlists
        if (showAddToPlaylistModal) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistModal = false },
                containerColor = Color(0x1AFFFFFF)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(com.example.ui.Translations.get(settings.appLanguage, "add_to_playlist"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    playlistsMap.keys.forEach { playlistName ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onAddToPlaylist(playlistName)
                                    showAddToPlaylistModal = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
                        ) {
                            Text(
                                text = playlistName,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%d:%02d", mins, secs)
}