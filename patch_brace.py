import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

bad_snippet = """            // Bottom Artist Info Tab
            if (!isLandscape) {
                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Modal to add song to custom playlists"""

good_snippet = """            // Bottom Artist Info Tab
            if (!isLandscape) {
                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }

        // Modal to add song to custom playlists"""

content = content.replace(bad_snippet, good_snippet)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
