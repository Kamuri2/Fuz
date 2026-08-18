import re
with open('app/src/main/java/com/example/ui/components/LiquidGlassBackground.kt', 'r') as f:
    content = f.read()

if "import com.example.ui.components.TrackImage" not in content:
    content = content.replace("import coil.compose.AsyncImage", "import coil.compose.AsyncImage\nimport com.example.ui.components.TrackImage")

old = """                currentTrack.albumArtUri != null -> {
                    AsyncImage(
                        model = currentTrack.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = orbScale
                                scaleY = orbScale
                                translationX = bgOffsetX
                                translationY = bgOffsetY
                            }
                            .blur(75.dp)
                    )
                }"""
new = """                true -> {
                    TrackImage(
                        track = currentTrack,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = orbScale
                                scaleY = orbScale
                                translationX = bgOffsetX
                                translationY = bgOffsetY
                            }
                            .blur(75.dp)
                    )
                }"""

content = content.replace(old, new)
with open('app/src/main/java/com/example/ui/components/LiquidGlassBackground.kt', 'w') as f:
    f.write(content)
