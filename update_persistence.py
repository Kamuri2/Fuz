import re

with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'r') as f:
    content = f.read()

# I want to save the current track when it changes
save_track_logic = """
    private fun saveLastTrackId(trackId: Long?) {
        try {
            if (trackId != null) {
                prefs.edit().putLong("last_track_id", trackId).apply()
            }
        } catch (e: Exception) {}
    }
"""

# Insert saveLastTrackId before loadFavorites
content = content.replace("    private fun loadFavorites(): Set<Long> {", save_track_logic + "\n    private fun loadFavorites(): Set<Long> {")

# When _currentTrack is set, we call saveLastTrackId
play_track_content = "        _currentTrack.value = track"
play_track_new_content = "        _currentTrack.value = track\n        saveLastTrackId(track.id)"
content = content.replace(play_track_content, play_track_new_content)

# In getInstance, how do we load it?
# Actually, since AudioPlayerManager doesn't have the tracks list internally (it relies on MainActivity passing it through),
# we can't easily auto-load the track by ID unless we query TrackRepository.
# Let's add an init block or load it directly in the background.

init_logic = """
    init {
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
    }
"""

content = content.replace("    private val prefs = context.getSharedPreferences(\"app_settings_prefs\", android.content.Context.MODE_PRIVATE)", "    private val prefs = context.getSharedPreferences(\"app_settings_prefs\", android.content.Context.MODE_PRIVATE)\n" + init_logic)

with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'w') as f:
    f.write(content)
