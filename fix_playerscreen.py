import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if skip:
        if "Icon(" in line and "96.dp" in lines[i+4]:
            pass
        elif "}" in line and "}" in lines[i-1] and "}" in lines[i-2]:
            skip = False
        continue

    if "val extension = if (currentTrack != null) {" in line and "com.example.ui.components" in line:
        new_lines.append('            val extension = currentTrack.path.substringAfterLast(".", "flac").uppercase()\n')
        new_lines.append('            val size = currentTrack.fileSizeFormatted\n')
        new_lines.append('            "$extension  |  $size  |  44.1 KHZ"\n')
        continue
    
    if "if (currentTrack != null) { com.example.ui.components.TrackImage" in line and not "val extension" in line:
        new_lines.append('                        if (currentTrack != null) {\n')
        new_lines.append('                            com.example.ui.components.TrackImage(track = currentTrack, modifier = Modifier.fillMaxSize())\n')
        new_lines.append('                        } else {\n')
        new_lines.append('                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(96.dp))\n')
        new_lines.append('                        }\n')
        skip = True
        continue
    
    if "artworkBitmap != null" in line:
        continue
    if "bitmap = artworkBitmap!!," in line:
        continue

    new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.writelines(new_lines)
