with open('app/src/main/java/com/example/ui/components/QueueBottomSheet.kt', 'r') as f:
    content = f.read()

content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old = """                    track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())"""
new = """                    true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())"""

content = content.replace(old, new)

with open('app/src/main/java/com/example/ui/components/QueueBottomSheet.kt', 'w') as f:
    f.write(content)
