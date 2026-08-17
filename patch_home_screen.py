with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old_image = """                artworkBitmap != null -> Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))"""

new_image = """                true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())"""

content = content.replace(old_image, new_image)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
