package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.AppSettings
import com.example.model.AppTheme
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LoopMode {
    OFF, REPEAT_ALL, REPEAT_ONE
}

enum class EqPreset(val displayName: String, val bass: Float, val mid: Float, val treble: Float) {
    FLAT("Normal / Flat", 1.0f, 1.0f, 1.0f),
    BASS_BOOST("Liquid Bass Boost", 1.6f, 0.9f, 0.9f),
    VOCAL("Crystal Vocal", 0.8f, 1.4f, 1.2f),
    ELECTRONIC("Neon Synth / EDM", 1.4f, 1.1f, 1.5f),
    ROCK("Power Rock", 1.3f, 0.8f, 1.4f),
    ACOUSTIC("Acoustic Glass", 1.1f, 1.2f, 1.1f)
}

class AudioPlayerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerManager"
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)




    private var mediaPlayer: MediaPlayer? = null
    private var fadingPlayer: MediaPlayer? = null
    private var activeSessionId = 0L
    private var autoAdvanceTriggeredSessionId = -1L
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        scope.launch {
            val lastTrackId = prefs.getLong("last_track_id", -1L)
            if (lastTrackId != -1L) {
                try {
                    // Wait for tracks to be loaded
                    val tracks = com.example.data.TrackRepository.tracks.first { it.isNotEmpty() }
                    if (_currentTrack.value == null) {
                        val lastTrack = tracks.find { it.id == lastTrackId }
                        if (lastTrack != null) {
                            _playlist.value = listOf(lastTrack)
                            _currentIndex.value = 0
                            _currentTrack.value = lastTrack
                            _durationMs.value = lastTrack.durationMs
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Track Queue State
    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.REPEAT_ALL)
    val loopMode: StateFlow<LoopMode> = _loopMode.asStateFlow()

    private val _eqPreset = MutableStateFlow(EqPreset.FLAT)
    val eqPreset: StateFlow<EqPreset> = _eqPreset.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(loadFavorites())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()


    private fun saveLastTrackId(trackId: Long?) {
        try {
            if (trackId != null) {
                prefs.edit().putLong("last_track_id", trackId).apply()
            }
        } catch (e: Exception) {}
    }

    private fun loadFavorites(): Set<Long> {
        return try {
            prefs.getStringSet("favorite_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveFavorites(favs: Set<Long>) {
        try {
            prefs.edit().putStringSet("favorite_ids", favs.map { it.toString() }.toSet()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorites: ${e.message}")
        }
    }

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _selectedEqPreset = MutableStateFlow(EqPreset.BASS_BOOST)
    val selectedEqPreset: StateFlow<EqPreset> = _selectedEqPreset.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    // App Settings State
    private val _appSettings = MutableStateFlow(loadSettings())
    val appSettings: StateFlow<com.example.model.AppSettings> = _appSettings.asStateFlow()

    private fun loadSettings(): com.example.model.AppSettings {
        return try {
            com.example.model.AppSettings(
                isDarkMode = prefs.getBoolean("isDarkMode", true),
                appLanguage = prefs.getString("appLanguage", "English") ?: "English",
                selectedTheme = try { com.example.model.AppTheme.valueOf(prefs.getString("selectedTheme", "SUNSET") ?: "SUNSET") } catch(e: Exception) { com.example.model.AppTheme.SUNSET },
                fontFamilyName = prefs.getString("fontFamilyName", "System Font (Default)") ?: "System Font (Default)",
                lyricsFontSizePercent = prefs.getInt("lyricsFontSizePercent", 110),
                isLyricsTranslationEnabled = prefs.getBoolean("isLyricsTranslationEnabled", false),
                targetTranslationLanguage = prefs.getString("targetTranslationLanguage", "Spanish") ?: "Spanish",
                crossfadeDuration = prefs.getFloat("crossfadeDuration", 0f)
            )
        } catch (e: Exception) {
            com.example.model.AppSettings()
        }
    }

    private fun saveSettings(settings: com.example.model.AppSettings) {
        try {
            prefs.edit().apply {
                putBoolean("isDarkMode", settings.isDarkMode)
                putString("appLanguage", settings.appLanguage)
                putString("selectedTheme", settings.selectedTheme.name)
                putString("fontFamilyName", settings.fontFamilyName)
                putInt("lyricsFontSizePercent", settings.lyricsFontSizePercent)
                putBoolean("isLyricsTranslationEnabled", settings.isLyricsTranslationEnabled)
                putString("targetTranslationLanguage", settings.targetTranslationLanguage)
                putFloat("crossfadeDuration", settings.crossfadeDuration)
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings: ${e.message}")
        }
    }

    // Disliked Tracks State
    private val _dislikedTracks = MutableStateFlow<Set<Long>>(loadDislikes())
    val dislikedTracks: StateFlow<Set<Long>> = _dislikedTracks.asStateFlow()

    private fun loadDislikes(): Set<Long> {
        return try {
            prefs.getStringSet("disliked_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveDislikes(dislikes: Set<Long>) {
        try {
            prefs.edit().putStringSet("disliked_ids", dislikes.map { it.toString() }.toSet()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dislikes: ${e.message}")
        }
    }

    // Playlists State
    private val _playlistsMap = kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<com.example.model.Track>>>(
        mapOf("Relax" to emptyList(), "Entrenamiento" to emptyList())
    )
    val playlistsMap: kotlinx.coroutines.flow.StateFlow<Map<String, List<com.example.model.Track>>> = _playlistsMap.asStateFlow()

    fun updateSettings(newSettings: com.example.model.AppSettings) {
        _appSettings.value = newSettings
        saveSettings(newSettings)
    }

    fun updateTheme(theme: AppTheme) {
        updateSettings(_appSettings.value.copy(selectedTheme = theme))
    }

    fun toggleDarkMode() {
        updateSettings(_appSettings.value.copy(isDarkMode = !_appSettings.value.isDarkMode))
    }

    fun setAppLanguage(lang: String) {
        updateSettings(_appSettings.value.copy(appLanguage = lang))
    }

    fun setLyricsFontSize(percent: Int) {
        updateSettings(_appSettings.value.copy(lyricsFontSizePercent = percent))
    }

    fun toggleLyricsTranslation() {
        updateSettings(_appSettings.value.copy(isLyricsTranslationEnabled = !_appSettings.value.isLyricsTranslationEnabled))
    }

    fun toggleDislike(trackId: Long) {
        val set = _dislikedTracks.value.toMutableSet()
        if (set.contains(trackId)) {
            set.remove(trackId)
        } else {
            set.add(trackId)
        }
        _dislikedTracks.value = set
        saveDislikes(set)
    }

    fun removeFromQueue(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index in currentList.indices) {
            val isCurrentBeingRemoved = index == _currentIndex.value
            currentList.removeAt(index)
            _playlist.value = currentList
            if (currentList.isEmpty()) {
                _currentIndex.value = -1
                _currentTrack.value = null
                mediaPlayer?.reset()
                _isPlaying.value = false
            } else if (isCurrentBeingRemoved) {
                val nextIdx = index.coerceAtMost(currentList.lastIndex)
                playTrackAtIndex(nextIdx)
            } else if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }
    }

    fun setPlayNext(track: Track) {
        val currentList = _playlist.value.toMutableList()
        val currIdx = _currentIndex.value
        val existingIdx = currentList.indexOfFirst { it.id == track.id }
        if (existingIdx != -1) {
            currentList.removeAt(existingIdx)
        }
        val insertPos = if (currIdx in currentList.indices) currIdx + 1 else currentList.size
        currentList.add(insertPos, track)
        _playlist.value = currentList
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val map = _playlistsMap.value.toMutableMap()
        if (!map.containsKey(name)) {
            map[name] = emptyList()
            _playlistsMap.value = map
        }
    }

    fun addToPlaylist(playlistName: String, track: Track) {
        val map = _playlistsMap.value.toMutableMap()
        val list = map[playlistName]?.toMutableList() ?: mutableListOf()
        if (!list.contains(track)) {
            list.add(track)
            map[playlistName] = list
            _playlistsMap.value = map
        }
    }

    init {
        initMediaPlayer()
    }



    private fun initMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }






    private var crossfadeJob: Job? = null

    private fun doCrossfade(fromPlayer: MediaPlayer?, toPlayer: MediaPlayer, durationMs: Long) {
        val effectiveDuration = durationMs.coerceAtLeast(300L)
        val steps = 25
        val stepDuration = (effectiveDuration / steps).coerceAtLeast(16L)
        val targetVolume = _volume.value
        
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch(Dispatchers.Main) {
            for (i in 1..steps) {
                val fraction = i.toFloat() / steps.toFloat()
                val fadeOutVol = targetVolume * (1f - fraction)
                val fadeInVol = targetVolume * fraction
                
                try {
                    fromPlayer?.setVolume(fadeOutVol, fadeOutVol)
                    toPlayer.setVolume(fadeInVol, fadeInVol)
                } catch (e: Exception) {}
                
                delay(stepDuration)
            }
            
            try {
                fromPlayer?.stop()
                fromPlayer?.release()
            } catch (e: Exception) {}
            if (fromPlayer == fadingPlayer) {
                fadingPlayer = null
            }
            try {
                toPlayer.setVolume(targetVolume, targetVolume)
            } catch (e: Exception) {}
        }
    }

    fun setQueue(tracks: List<Track>, startTrackIndex: Int = 0, autoPlay: Boolean = true) {
        _isShuffle.value = false
        _playlist.value = tracks
        if (tracks.isNotEmpty() && startTrackIndex in tracks.indices) {
            if (autoPlay) {
                playTrackAtIndex(startTrackIndex)
            } else {
                val track = tracks[startTrackIndex]
                _currentIndex.value = startTrackIndex
                _currentTrack.value = track
        saveLastTrackId(track.id)
                try {
                    mediaPlayer?.reset()
                    mediaPlayer?.setDataSource(context, track.contentUri)
                    mediaPlayer?.prepareAsync()
                } catch (e: Exception) {}
            }
        }
    }

    fun playTrack(track: Track) {
        val index = _playlist.value.indexOfFirst { it.id == track.id }
        if (index != -1) {
            playTrackAtIndex(index)
        } else {
            val newPlaylist = _playlist.value + track
            _playlist.value = newPlaylist
            playTrackAtIndex(newPlaylist.lastIndex)
        }
    }

    fun playTrackAtIndex(index: Int) {
        val tracks = _playlist.value
        if (index !in tracks.indices) return

        val track = tracks[index]
        _currentIndex.value = index
        _currentTrack.value = track
        saveLastTrackId(track.id)

        val currentSession = ++activeSessionId

        // Asynchronously enrich metadata on IO thread without blocking UI or playback start
        scope.launch(Dispatchers.IO) {
            try {
                val enriched = com.example.data.MetadataReader.extractFullMetadata(context, track)
                if (_currentIndex.value == index) {
                    _currentTrack.value = enriched
                }
                val currentPlaylist = _playlist.value.toMutableList()
                if (index in currentPlaylist.indices && currentPlaylist[index].id == track.id) {
                    currentPlaylist[index] = enriched
                    _playlist.value = currentPlaylist
                }
                var finalEnriched = enriched
                if (finalEnriched.lyrics.isBlank()) {
                    // Fallback to internet if local extraction yielded nothing
                    val onlineLyrics = com.example.data.LrcLibHelper.fetchLyrics(track.title, track.artist, track.album, track.durationMs / 1000)
                    if (onlineLyrics != null && onlineLyrics.isNotBlank()) {
                        finalEnriched = finalEnriched.copy(lyrics = onlineLyrics)
                        if (_currentIndex.value == index) {
                            _currentTrack.value = finalEnriched
                        }
                        if (index in currentPlaylist.indices && currentPlaylist[index].id == track.id) {
                            currentPlaylist[index] = finalEnriched
                            _playlist.value = currentPlaylist
                        }
                    }
                }
                
                if (finalEnriched.lyrics.isNotBlank()) {
                    com.example.data.TrackRepository.updateTrackLyrics(context, track.id, finalEnriched.lyrics)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Metadata extraction skipped: ${e.message}")
            }
        }

        try {
            val intent = android.content.Intent(context, PlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PlaybackService: ${e.message}")
        }

        try {
            crossfadeJob?.cancel()
            
            // Dispose existing fading player immediately to prevent pile-up
            try {
                fadingPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
            } catch (e: Exception) {}
            fadingPlayer = null

            val oldPlayer = mediaPlayer
            var canCrossfade = false
            val cfDuration = (_appSettings.value.crossfadeDuration * 1000).toLong()

            if (oldPlayer != null) {
                oldPlayer.setOnCompletionListener(null)
                try {
                    if (cfDuration > 0 && oldPlayer.isPlaying) {
                        fadingPlayer = oldPlayer
                        canCrossfade = true
                    } else {
                        oldPlayer.stop()
                        oldPlayer.release()
                    }
                } catch (e: Exception) {
                    try { oldPlayer.release() } catch (ex: Exception) {}
                }
            }
            mediaPlayer = null

            val newPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // Set volume to 0 upfront if crossfading to prevent burst of full volume
                if (canCrossfade) {
                    setVolume(0f, 0f)
                } else {
                    setVolume(_volume.value, _volume.value)
                }
                setOnCompletionListener {
                    if (currentSession == activeSessionId) {
                        handleTrackCompletion()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    if (currentSession == activeSessionId) {
                        _isPlaying.value = false
                        nextTrack()
                    }
                    true
                }
            }
            mediaPlayer = newPlayer
            
            newPlayer.setDataSource(context, track.contentUri)
            newPlayer.setOnPreparedListener { mp ->
                if (activeSessionId != currentSession || mediaPlayer != mp) {
                    try { mp.release() } catch(e: Exception) {}
                    return@setOnPreparedListener
                }
                
                try {
                    mp.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start player: ${e.message}")
                }
                
                if (canCrossfade && fadingPlayer != null) {
                    val from = fadingPlayer
                    var remainingFrom = cfDuration
                    try {
                        val pos = from?.currentPosition?.toLong() ?: 0L
                        val dur = from?.duration?.toLong() ?: 0L
                        if (dur > pos) {
                            remainingFrom = (dur - pos).coerceIn(500L, cfDuration)
                        }
                    } catch (e: Exception) {}

                    doCrossfade(from, mp, remainingFrom)
                } else {
                    mp.setVolume(_volume.value, _volume.value)
                    try {
                        fadingPlayer?.let {
                            if (it.isPlaying) it.stop()
                            it.release()
                        }
                    } catch(e: Exception) {}
                    fadingPlayer = null
                }
                
                _isPlaying.value = true
                try {
                    _durationMs.value = mp.duration.toLong().coerceAtLeast(1L)
                } catch (e: Exception) {}
                startProgressTracker(currentSession)
            }
            newPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play track ${track.title}: ${e.message}")
            try { mediaPlayer?.release() } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp == null) {
            // If the player is null but we have a track in the playlist, try to play it
            if (_currentIndex.value in _playlist.value.indices) {
                playTrackAtIndex(_currentIndex.value)
            } else if (_playlist.value.isNotEmpty()) {
                playTrackAtIndex(0)
            }
            return
        }
        if (_currentTrack.value == null && _playlist.value.isNotEmpty()) {
            playTrackAtIndex(0)
            return
        }

        try {
            if (mp.isPlaying) {
                mp.pause()
                try { fadingPlayer?.pause() } catch(e: Exception) {}
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                mp.start()
                try { fadingPlayer?.start() } catch(e: Exception) {}
                _isPlaying.value = true
                startProgressTracker(activeSessionId)
            }
        } catch (e: Exception) {
            // If the player is in an invalid state, reload the track completely
            Log.e(TAG, "togglePlayPause error: ${e.message}, reloading track")
            if (_currentIndex.value in _playlist.value.indices) {
                playTrackAtIndex(_currentIndex.value)
            }
        }
    }

    fun nextTrack() {
        val tracks = _playlist.value
        if (tracks.isEmpty()) return

        val nextIndex = (_currentIndex.value + 1) % tracks.size
        playTrackAtIndex(nextIndex)
    }

    fun previousTrack() {
        val tracks = _playlist.value
        if (tracks.isEmpty()) return

        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 < 0) tracks.lastIndex else _currentIndex.value - 1
        playTrackAtIndex(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        try {
            crossfadeJob?.cancel()
            try {
                fadingPlayer?.stop()
                fadingPlayer?.release()
            } catch(e: Exception) {}
            fadingPlayer = null

            mediaPlayer?.seekTo(positionMs.toInt())
            mediaPlayer?.setVolume(_volume.value, _volume.value)
        } catch (e: Exception) {
            Log.e(TAG, "Seek error: ${e.message}")
        }
        _currentPositionMs.value = positionMs
    }

    fun setVolume(vol: Float) {
        _volume.value = vol
        try {
            mediaPlayer?.setVolume(vol, vol)
            fadingPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e(TAG, "Volume change error: ${e.message}")
        }
    }

    fun setShuffle(enabled: Boolean) {
        if (_isShuffle.value != enabled) {
            toggleShuffle()
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        val current = _currentTrack.value
        if (_isShuffle.value) {
            val currentPlaylist = _playlist.value.toMutableList()
            val shuffled = currentPlaylist.shuffled().toMutableList()
            if (current != null) {
                shuffled.remove(current)
                shuffled.add(0, current)
                _currentIndex.value = 0
            }
            _playlist.value = shuffled
        }
    }

    fun cycleLoopMode() {
        _loopMode.value = when (_loopMode.value) {
            LoopMode.OFF -> LoopMode.REPEAT_ALL
            LoopMode.REPEAT_ALL -> LoopMode.REPEAT_ONE
            LoopMode.REPEAT_ONE -> LoopMode.OFF
        }
    }

    fun toggleFavorite(trackId: Long) {
        val set = _favorites.value.toMutableSet()
        if (set.contains(trackId)) {
            set.remove(trackId)
        } else {
            set.add(trackId)
        }
        _favorites.value = set
        saveFavorites(set)
    }

    fun setEqPreset(preset: EqPreset) {
        _selectedEqPreset.value = preset
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                delay(minutes * 60_000L)
                if (_isPlaying.value) {
                    togglePlayPause()
                }
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun handleTrackCompletion() {
        when (_loopMode.value) {
            LoopMode.REPEAT_ONE -> {
                seekTo(0)
                mediaPlayer?.start()
            }
            LoopMode.REPEAT_ALL -> {
                nextTrack()
            }
            LoopMode.OFF -> {
                if (_currentIndex.value < _playlist.value.lastIndex) {
                    nextTrack()
                } else {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
        }
    }

    private fun startProgressTracker(sessionId: Long = activeSessionId) {
        stopProgressTracker()
        progressJob = scope.launch {
            while (true) {
                if (sessionId != activeSessionId) break
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val currentPos = mp.currentPosition.toLong()
                        val duration = mp.duration.toLong().coerceAtLeast(1L)
                        _currentPositionMs.value = currentPos
                        _durationMs.value = duration
                        
                        val cfDuration = (_appSettings.value.crossfadeDuration * 1000).toLong()
                        if (cfDuration > 0 && autoAdvanceTriggeredSessionId != sessionId) {
                            val triggerThreshold = cfDuration + 1200L
                            if (duration - currentPos <= triggerThreshold && duration > triggerThreshold + 1000L) {
                                autoAdvanceTriggeredSessionId = sessionId
                                handleTrackCompletion()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        sleepTimerJob?.cancel()
        crossfadeJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch(e: Exception) {}
        mediaPlayer = null
        
        try {
            fadingPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch(e: Exception) {}
        fadingPlayer = null
    }
}
