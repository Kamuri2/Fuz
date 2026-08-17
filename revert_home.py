with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

old_block = """        ) {
            when {
                true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())
            }
        }"""
new_block = """        ) {
            when {
                track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else -> Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
            }
        }"""
content = content.replace(old_block, new_block)
with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
