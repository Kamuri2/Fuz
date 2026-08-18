import re
with open('app/src/main/java/com/example/data/AudioScanner.kt', 'r') as f:
    content = f.read()

content = content.replace("albumArtUri = artUri,", "albumArtUri = null, // Disable MediaStore folder fallback")

with open('app/src/main/java/com/example/data/AudioScanner.kt', 'w') as f:
    f.write(content)
