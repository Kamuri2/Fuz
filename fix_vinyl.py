import re

with open('app/src/main/java/com/example/ui/components/LiquidVinylArtwork.kt', 'r') as f:
    content = f.read()

if "import com.example.ui.components.TrackImage" not in content:
    content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old = """                    track?.albumArtUri != null -> {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = track.album,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }"""
new = """                    track != null -> {
                        TrackImage(
                            track = track,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }"""

content = content.replace(old, new)

with open('app/src/main/java/com/example/ui/components/LiquidVinylArtwork.kt', 'w') as f:
    f.write(content)
