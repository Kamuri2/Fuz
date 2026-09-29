package com.example.ui.screens

import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LrcLine
import com.example.data.LrcParser
import com.example.model.Track
import com.example.player.LoopMode
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ArtistInfoTab
import com.example.ui.components.WaveformSeekBar
import com.example.ui.theme.GlassTextMuted
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
    playlists: List<com.example.data.local.PlaylistEntity> = emptyList(),
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onLoopCycle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDislikeToggle: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit = {},
    onCreatePlaylistAndAdd: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLyricsMode by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val trackId = currentTrack?.id ?: -1L
    var oldTrackId by remember { mutableStateOf(-1L) }
    var slideDirection by remember { mutableStateOf(1) }

    LaunchedEffect(trackId) {
        if (oldTrackId != -1L && oldTrackId != trackId) {
            slideDirection = 1
        }
        oldTrackId = trackId
    }

    var showAddToPlaylistModal by remember { mutableStateOf(false) }
    
    var artistInfo by remember { mutableStateOf<com.example.data.ArtistInfo?>(null) }
    LaunchedEffect(currentTrack?.artist) {
        artistInfo = null
        currentTrack?.artist?.let { artistName ->
            artistInfo = com.example.data.ArtistInfoFetcher.fetchArtistInfo(artistName)
        }
    }

    val parsedLyrics = remember(currentTrack?.lyrics) {
        com.example.model.LyricsParser.parseLrc(currentTrack?.lyrics ?: "")
    }

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableStateOf(0f) }

    val effectiveDuration = if (durationMs > 0L) durationMs else (currentTrack?.durationMs ?: 0L)
    val displayPositionMs = if (isSeeking) (seekFraction * effectiveDuration).toLong() else currentPositionMs
    val currentProgress = if (isSeeking) seekFraction else (if (effectiveDuration > 0L) (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f) else 0f)

    // artworkBitmap removed in favor of TrackImage

    val formatTag = remember(currentTrack) {
        if (currentTrack == null) "FLAC  988 KBPS  44.1 KHZ"
        else {
            val extension = when {
                currentTrack.path.endsWith(".flac", true) -> "FLAC"
                currentTrack.path.endsWith(".mp3", true) -> "MP3"
                currentTrack.path.endsWith(".m4a", true) || currentTrack.path.endsWith(".aac", true) -> "AAC"
                currentTrack.path.endsWith(".wav", true) -> "WAV"
                currentTrack.path.endsWith(".opus", true) -> "OPUS"
                currentTrack.path.endsWith(".ogg", true) -> "OGG"
                else -> "FLAC"
            }
            // Use fallback strings if bitrate/samplerate properties don't exist
            "$extension  |  ${currentTrack.fileSizeFormatted}  |  44.1 KHZ"
        }
    }

    LiquidGlassBackground(isPlaying = isPlaying, currentTrack = currentTrack, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
            // ==================== LANDSCAPE MODE ====================
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).testTag("close_player_landscape")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Deslizar para cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // LEFT SIDE: Cover, Title, Artist - Album, and Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Artwork Cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentTrack != null) {
                            com.example.ui.components.PlayerAlbumArt(track = currentTrack, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(96.dp))
                        }
                        
                        // Action Icons Row overlaid at the bottom of the album art
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                                    )
                                )
                                .padding(vertical = 8.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("player_favorite_btn")) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = onDislikeToggle) {
                                Icon(
                                    imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                    contentDescription = "Dislike",
                                    tint = if (isDisliked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = { showAddToPlaylistModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to playlist",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Lyrics Mic Icon Toggle
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (showLyricsMode) Color.White.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { showLyricsMode = !showLyricsMode },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Lyrics",
                                    tint = if (showLyricsMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Song Title & Artist - Album
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentTrack?.title ?: "No Track Playing",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentTrack?.artist ?: "Unknown Artist"} - ${currentTrack?.album ?: "Unknown Album"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = GlassTextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }
                // RIGHT SIDE: Playback Controls OR Lyrics Display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!showLyricsMode) {
                        // Playback Controls View in Landscape
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Main Playback Controls Row (Rewind 10s, Previous, Big Play/Pause, Next, Forward 10s)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onSeek((currentPositionMs - 10000L).coerceAtLeast(0L)) }) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            Spacer(modifier = Modifier.height(24.dp))
                                IconButton(onClick = { slideDirection = -1; onPrevious() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                // Prominent Play/Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable { onPlayPauseToggle() }
                                        .testTag("player_main_play_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                                IconButton(onClick = { slideDirection = 1; onNext() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                IconButton(onClick = { onSeek(currentPositionMs + 10000L) }) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Waveform Progress Bar with Audio Soundwaves
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                WaveformSeekBar(
                                    progress = currentProgress,
                                    onProgressChange = { fraction ->
                                        isSeeking = true
                                        seekFraction = fraction
                                    },
                                    onProgressChangeFinished = {
                                        isSeeking = false
                                        val target = (seekFraction * effectiveDuration).toLong().coerceIn(0L, effectiveDuration)
                                        onSeek(target)
                                    },
                                    isPlaying = isPlaying,
                                    trackId = currentTrack?.id ?: 0L,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    height = 42.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatDuration(displayPositionMs),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GlassTextMuted
                                    )
                                    Text(
                                        text = formatDuration(durationMs),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GlassTextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))


                            // Secondary Control Icons (Mic/Lyrics, Repeat, Shuffle)
                            Row(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onShuffleToggle) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Lyrics Toggle Button (Mic)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (showLyricsMode) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable { showLyricsMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Lyrics",
                                        tint = if (showLyricsMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(onClick = onLoopCycle) {
                                    Icon(
                                        imageVector = if (loopMode == LoopMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                        contentDescription = "Repeat",
                                        tint = if (loopMode != LoopMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Lyrics View replacing Controls on Right Side in Landscape
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x1A000000))
                                .padding(12.dp)
                        ) {
                            // Header of Lyrics View with Close Button to return to controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = com.example.ui.Translations.get(settings.appLanguage, "lyrics") ?: "Letras",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { showLyricsMode = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar letras",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Lyrics Content Display
                            LyricsContent(
                                parsedLyrics = parsedLyrics,
                                rawLyrics = currentTrack?.lyrics ?: "",
                                currentPositionMs = currentPositionMs,
                                language = settings.appLanguage,
                                onSeek = onSeek,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                }
            }
            }
        } else {
            // ==================== PORTRAIT MODE ====================
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
                    .padding(top = 8.dp, bottom = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header with Swipe Down Handle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 0.dp)
                        .clickable { onBack() }
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Deslizar para cerrar",
                            tint = GlassTextMuted,
                            modifier = Modifier.size(18.dp)
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

                // Breathing space
                Spacer(modifier = Modifier.height(2.dp))

                AnimatedContent(
                    targetState = currentTrack?.id ?: 0L,
                    transitionSpec = {
                        val duration = 220
                        if (slideDirection >= 0) {
                            (slideInHorizontally(androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(androidx.compose.animation.core.tween(duration)))
                                .togetherWith(slideOutHorizontally(androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(androidx.compose.animation.core.tween(duration)))
                        } else {
                            (slideInHorizontally(androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(androidx.compose.animation.core.tween(duration)))
                                .togetherWith(slideOutHorizontally(androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(androidx.compose.animation.core.tween(duration)))
                        }
                    },
                    label = "Track Transition",
                    modifier = Modifier.weight(1f)
                ) { _ ->
                    val targetTrack = currentTrack
                    Crossfade(
                        targetState = showLyricsMode,
                        label = "LyricsModeToggle",
                        modifier = Modifier.fillMaxSize()
                    ) { isLyricsMode ->
                        if (!isLyricsMode) {
                            // ==================== ALBUM ART MODE ====================
                            // 1:1 Square Album Art Area with Action Buttons overlaid at bottom
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.97f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1AFFFFFF))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (targetTrack != null) {
                                    com.example.ui.components.PlayerAlbumArt(
                                        track = targetTrack,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(96.dp)
                                    )
                                }
                            }

                            // Action Icons Row overlaid at the bottom of the album art
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                                        )
                                    )
                                    .padding(vertical = 8.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("player_favorite_btn")) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = onDislikeToggle) {
                                    Icon(
                                        imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                        contentDescription = "Dislike",
                                        tint = if (isDisliked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = { showAddToPlaylistModal = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to playlist",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Lyrics Mic Icon Toggle
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (showLyricsMode) Color.White.copy(alpha = 0.35f) else Color.Transparent)
                                        .clickable { showLyricsMode = !showLyricsMode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Lyrics",
                                        tint = if (showLyricsMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        }
                    } else {
                        // ==================== LYRICS VIEW MODE ====================
                        // Cover is hidden! Full lyrics view occupies the center stage
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x14FFFFFF))
                            ) {
                                LyricsContent(
                                    parsedLyrics = parsedLyrics,
                                    rawLyrics = targetTrack?.lyrics ?: "",
                                    currentPositionMs = currentPositionMs,
                                    language = settings.appLanguage,
                                    onSeek = onSeek,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
// Action Icons Row below lyrics so user can toggle back anytime
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("player_favorite_btn")) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = onDislikeToggle) {
                                    Icon(
                                        imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                        contentDescription = "Dislike",
                                        tint = if (isDisliked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = { showAddToPlaylistModal = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to playlist",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Active Lyrics Mic Icon
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        .clickable { showLyricsMode = !showLyricsMode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Lyrics",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    } }
// end of if/else isLyricsMode
                    } // end of Crossfade block
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Compact Live Single-Line Lyric Snippet
                if (!showLyricsMode) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveSyncedLyricSnippet(
                            parsedLyrics = parsedLyrics,
                            rawLyrics = currentTrack?.lyrics ?: "",
                            currentPositionMs = currentPositionMs,
                            onClick = { showLyricsMode = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Title and Artist Centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = currentTrack?.title ?: "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTrack?.artist ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Waveform Progress Bar with Audio Soundwaves
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    WaveformSeekBar(
                        progress = currentProgress,
                        onProgressChange = { fraction ->
                            isSeeking = true
                            seekFraction = fraction
                        },
                        onProgressChangeFinished = {
                            isSeeking = false
                            val target = (seekFraction * effectiveDuration).toLong().coerceIn(0L, effectiveDuration)
                            onSeek(target)
                        },
                        isPlaying = isPlaying,
                        trackId = currentTrack?.id ?: 0L,
                        activeColor = MaterialTheme.colorScheme.primary,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatDuration(displayPositionMs), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GlassTextMuted)
                        Text(text = formatDuration(durationMs), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GlassTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { slideDirection = -1; onPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { onPlayPauseToggle() }
                            .testTag("player_main_play_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    IconButton(onClick = { slideDirection = 1; onNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onLoopCycle) {
                        Icon(
                            imageVector = if (loopMode == LoopMode.REPEAT_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (loopMode != LoopMode.OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))
}
            }
            
            // Bottom Artist Info Tab
            if (!isLandscape) {
                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    language = settings.appLanguage,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }

        // Modal to add song to custom playlists
        if (showAddToPlaylistModal) {
            var showNewPlaylistInput by remember { mutableStateOf(false) }
            var newPlaylistNameInput by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistModal = false },
                containerColor = Color(0xFF181818) // Clean Spotify-dark background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            com.example.ui.Translations.get(settings.appLanguage, "add_to_playlist"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showNewPlaylistInput = !showNewPlaylistInput }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create playlist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (showNewPlaylistInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlaylistNameInput,
                                onValueChange = { newPlaylistNameInput = it },
                                placeholder = { Text("Nombre de playlist...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistNameInput.isNotBlank()) {
                                        onCreatePlaylistAndAdd(newPlaylistNameInput.trim())
                                        newPlaylistNameInput = ""
                                        showNewPlaylistInput = false
                                        showAddToPlaylistModal = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Crear", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val userPlaylists = remember(playlists) {
                        playlists.filter { it.name != "All Songs" }
                    }

                    if (userPlaylists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay playlists creadas aún.\nToca el botón + para crear una.",
                                color = GlassTextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userPlaylists.size) { index ->
                                val pl = userPlaylists[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1FFFFFFF))
                                        .clickable {
                                            onAddToPlaylist(pl.playlistId)
                                            showAddToPlaylistModal = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2A2A2A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (pl.imageUri != null) {
                                            AsyncImage(
                                                model = android.net.Uri.parse(pl.imageUri),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!pl.description.isNullOrBlank()) {
                                            Text(
                                                text = pl.description,
                                                fontSize = 12.sp,
                                                color = GlassTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun LiveSyncedLyricSnippet(
    parsedLyrics: List<com.example.model.LyricsLine>,
    rawLyrics: String,
    currentPositionMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (parsedLyrics.isEmpty()) return

    val activeIndex = remember(currentPositionMs, parsedLyrics) {
        if (parsedLyrics.isEmpty()) -1
        else {
            val idx = parsedLyrics.indexOfLast { it.timestampMs <= currentPositionMs }
            if (idx != -1) idx else 0
        }
    }

    val currentLineText = remember(activeIndex, parsedLyrics) {
        if (activeIndex in parsedLyrics.indices) {
            parsedLyrics[activeIndex].text.trim()
        } else {
            ""
        }
    }

    if (currentLineText.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentLineText,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height / 2 } + fadeOut()
                )
            },
            label = "LiveLyricLine"
        ) { targetLine ->
            Text(
                text = targetLine,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LyricsContent(
    parsedLyrics: List<com.example.model.LyricsLine>,
    rawLyrics: String,
    currentPositionMs: Long,
    language: String,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (parsedLyrics.isEmpty()) {
        if (rawLyrics.isNotBlank()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rawLyrics,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = com.example.ui.Translations.get(language, "lyrics_not_available"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        val activeIndex = remember(currentPositionMs, parsedLyrics) {
            (if (parsedLyrics.isEmpty()) -1 else { val idx = parsedLyrics.indexOfLast { it.timestampMs <= currentPositionMs }; if (idx != -1) idx else 0 })
        }
        val listState = rememberLazyListState()

        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0 && !listState.isScrollInProgress) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items = parsedLyrics) { idx, line ->
                val isActive = idx == activeIndex
                val animatedColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isActive) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                    animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    label = "lyrics_color"
                )
                val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 0.95f,
                    animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    label = "lyrics_scale"
                )
                Text(
                    text = line.text,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = animatedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeek(line.timestampMs) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
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