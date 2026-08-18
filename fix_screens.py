import re
import os

def replace_in_file(path, regex, replacement):
    if not os.path.exists(path): return
    with open(path, 'r') as f:
        content = f.read()
    new_content = re.sub(regex, replacement, content, flags=re.MULTILINE)
    if new_content != content:
        if "import com.example.ui.components.TrackImage" not in new_content:
            new_content = new_content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")
        with open(path, 'w') as f:
            f.write(new_content)
        print(f"Updated {path}")

# HomeScreen: track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
replace_in_file('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 
                r'track\.albumArtUri != null -> AsyncImage\(model = track\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)', 
                r'true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())')

# QueueBottomSheet: track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
replace_in_file('app/src/main/java/com/example/ui/components/QueueBottomSheet.kt',
                r'track\.albumArtUri != null -> AsyncImage\(model = track\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)',
                r'true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())')

# LiquidVinylArtwork: AsyncImage(model = track.albumArtUri, ... modifier = Modifier.fillMaxSize())
replace_in_file('app/src/main/java/com/example/ui/components/LiquidVinylArtwork.kt',
                r'AsyncImage\(\s*model = track\.albumArtUri,\s*contentDescription = null,\s*contentScale = ContentScale\.Crop,\s*modifier = Modifier\.fillMaxSize\(\)\s*\)',
                r'TrackImage(track = track, modifier = Modifier.fillMaxSize())')

# MainActivity: track.albumArtUri != null -> AsyncImage(...)
replace_in_file('app/src/main/java/com/example/MainActivity.kt',
                r'track\.albumArtUri != null -> AsyncImage\(model = track\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)',
                r'true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())')

# AlbumsScreen: firstTrack?.albumArtUri != null -> AsyncImage(...)
replace_in_file('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt',
                r'firstTrack\?\.albumArtUri != null -> AsyncImage\(model = firstTrack\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)',
                r'firstTrack != null -> TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())')
replace_in_file('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt',
                r'AsyncImage\(model = firstTrack\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)',
                r'TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())')

# ArtistsScreen: (multiple)
replace_in_file('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt',
                r'AsyncImage\(model = track\.albumArtUri, contentDescription = null, contentScale = ContentScale\.Crop, modifier = Modifier\.fillMaxSize\(\)\)',
                r'TrackImage(track = track, modifier = Modifier.fillMaxSize())')

