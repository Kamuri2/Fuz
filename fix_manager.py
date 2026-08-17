import re

with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'r') as f:
    content = f.read()

# Remove the broken init block completely
bad_init = """    init {
        scope.launch(Dispatchers.IO) {
            val lastTrackId = prefs.getLong("last_track_id", -1L)
            if (lastTrackId != -1L) {
                try {
                    val tracks = com.example.data.TrackRepository.loadTracks(context)
                    val lastTrack = tracks.find { it.id == lastTrackId }
                    if (lastTrack != null) {
                        launch(Dispatchers.Main) {
                            _playlist.value = listOf(lastTrack)
                            _currentIndex.value = 0
                            _currentTrack.value = lastTrack
                            _durationMs.value = lastTrack.durationMs
                        }
                    }
                } catch(e: Exception) {}
            }
        }
    }"""
content = content.replace(bad_init, "")

# We need to add first import
content = content.replace("import kotlinx.coroutines.flow.asStateFlow", "import kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.flow.first")

good_init = """
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
"""

# Find where to put good_init. Let's put it right after scope initialization
scope_decl = "private val scope = CoroutineScope(Dispatchers.Main + Job())"
content = content.replace(scope_decl, scope_decl + "\n" + good_init)

with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'w') as f:
    f.write(content)
