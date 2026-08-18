import re
with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

old = """            val rawBytes = mmr.embeddedPicture
            if (rawBytes != null) {
                albumArtBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            }"""
new = """            var rawBytes = mmr.embeddedPicture
            if (rawBytes == null) {
                try {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(java.io.File(track.path))
                    val artwork = audioFile.tag?.firstArtwork
                    if (artwork != null) {
                        rawBytes = artwork.binaryData
                    }
                } catch (e: Exception) {}
            }
            if (rawBytes != null) {
                albumArtBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            }"""

content = content.replace(old, new)
with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
    f.write(content)
