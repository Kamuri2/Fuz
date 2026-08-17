with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'r') as f:
    content = f.read()

target = """                if (enriched.lyrics.isNotBlank()) {
                    com.example.data.TrackRepository.updateTrackLyrics(context, track.id, enriched.lyrics)
                }"""

replacement = """                var finalEnriched = enriched
                if (finalEnriched.lyrics.isBlank()) {
                    // Fallback to internet if local extraction yielded nothing
                    val onlineLyrics = com.example.data.LrcLibHelper.fetchLyrics(track.title, track.artist, track.album, (track.durationMs / 1000).toInt())
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
                }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/player/AudioPlayerManager.kt', 'w') as f:
    f.write(content)
