with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

old_block = """
        var albumArtBitmap: android.graphics.Bitmap? = null
        if (track.albumArtBytes != null) {
            albumArtBitmap = BitmapFactory.decodeByteArray(track.albumArtBytes, 0, track.albumArtBytes.size)
        } else if (track.albumArtUri != null) {
"""

new_block = """
        var albumArtBitmap: android.graphics.Bitmap? = null
        if (track.albumArtUri != null) {
"""

content = content.replace(old_block.strip(), new_block.strip())
with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
    f.write(content)
