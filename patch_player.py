import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

# 1. Add state variables near the top of PlayerScreen
import_insert = """import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.ArtistInfoTab"""

if "import com.example.ui.components.ArtistInfoTab" not in content:
    content = content.replace("import com.example.ui.components.LiquidGlassBackground", import_insert)

state_insert = """    var showAddToPlaylistModal by remember { mutableStateOf(false) }
    
    var artistInfo by remember { mutableStateOf<com.example.data.ArtistInfo?>(null) }
    LaunchedEffect(currentTrack?.artist) {
        artistInfo = null
        currentTrack?.artist?.let { artistName ->
            artistInfo = com.example.data.ArtistInfoFetcher.fetchArtistInfo(artistName)
        }
    }"""

content = content.replace("    var showAddToPlaylistModal by remember { mutableStateOf(false) }", state_insert)

# 2. Wrap the layout in a Box to overlay the tab
layout_replace = """    LiquidGlassBackground(isPlaying = isPlaying, currentTrack = currentTrack, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {"""

content = content.replace("    LiquidGlassBackground(isPlaying = isPlaying, currentTrack = currentTrack, modifier = modifier) {\n        if (isLandscape) {", layout_replace)

tab_insert = """            }
            
            // Bottom Artist Info Tab
            if (!isLandscape) {
                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Modal to add song to custom playlists"""

content = content.replace("            }\n        }\n\n        // Modal to add song to custom playlists", tab_insert)


with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
