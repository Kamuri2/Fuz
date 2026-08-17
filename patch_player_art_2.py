import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_art_3 = """                        when {
                            artworkBitmap != null -> {
                                Image(
                                    bitmap = artworkBitmap!!,
                                    contentDescription = currentTrack?.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            currentTrack?.albumArtUri != null -> {
                                AsyncImage(
                                    model = currentTrack.albumArtUri,
                                    contentDescription = currentTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(96.dp)
                                )
                            }
                        }"""

new_art_3 = """                        if (currentTrack != null) {
                            com.example.ui.components.TrackImage(
                                track = currentTrack,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(96.dp)
                            )
                        }"""

content = content.replace(old_art_3, new_art_3)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
