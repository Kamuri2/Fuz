import re
with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'r') as f:
    content = f.read()

old = """                                val trackFallback = tracks.firstOrNull { it.artist == artistName }?.albumArtUri
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (trackFallback != null) {
                                    AsyncImage(
                                        model = trackFallback,
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {"""
new = """                                val trackFallback = tracks.firstOrNull { it.artist == artistName }
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (trackFallback != null) {
                                    TrackImage(
                                        track = trackFallback,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {"""
content = content.replace(old, new)
with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'w') as f:
    f.write(content)
