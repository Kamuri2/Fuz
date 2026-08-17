with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old_image_1 = """                                        firstTrack?.albumArtUri != null -> AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())"""
new_image_1 = """                                        firstTrack != null -> TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())"""
content = content.replace(old_image_1, new_image_1)

old_image_2 = """                } else if (firstTrack?.albumArtUri != null) {
                    AsyncImage(model = firstTrack.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {"""
new_image_2 = """                } else if (firstTrack != null) {
                    TrackImage(track = firstTrack, modifier = Modifier.fillMaxSize())
                } else {"""
content = content.replace(old_image_2, new_image_2)

with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'w') as f:
    f.write(content)
