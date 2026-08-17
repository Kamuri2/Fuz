import os
import re

def process_file(filepath, replacements):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)

# 1. Update Track.kt
process_file('app/src/main/java/com/example/model/Track.kt', [
    ('val albumArtBytes: ByteArray? = null,', '')
])

# 2. Update TrackEntity.kt
process_file('app/src/main/java/com/example/data/local/TrackEntity.kt', [
    ('albumArtBytes = null,', '')
])

# 3. Update MetadataReader.kt
process_file('app/src/main/java/com/example/data/MetadataReader.kt', [
    ('albumArtBytes = artworkBytes,', ''),
    ('var artworkBytes: ByteArray? = null', 'var artworkBytes: ByteArray? = null // Removed from Track to save RAM')
])
