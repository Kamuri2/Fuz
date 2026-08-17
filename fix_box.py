import re
with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_block = """                        if (currentTrack != null) {
                            com.example.ui.components.TrackImage(track = currentTrack, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(96.dp))
                        }
                    Spacer(modifier = Modifier.height(16.dp))"""

new_block = """                        if (currentTrack != null) {
                            com.example.ui.components.TrackImage(track = currentTrack, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(96.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
