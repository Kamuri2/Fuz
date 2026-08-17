with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old_image_4 = """                    } else if (track.albumArtUri != null) {
                        AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {"""
new_image_4 = """                    } else {
                        TrackImage(track = track, modifier = Modifier.fillMaxSize())
                    }"""
content = content.replace(old_image_4, new_image_4)

with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'w') as f:
    f.write(content)
