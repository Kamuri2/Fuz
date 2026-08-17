with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("firstTrack != null -> TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())", "firstTrack?.albumArtUri != null -> AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())")

content = content.replace("} else if (firstTrack != null) {\n                    TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())", "} else if (firstTrack?.albumArtUri != null) {\n                    AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())")

with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'w') as f:
    f.write(content)
