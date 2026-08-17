import re
with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_block = """    val formatTag = remember(currentTrack) {
        if (currentTrack == null) "FLAC  988 KBPS  44.1 KHZ"
        else {
            val extension = currentTrack.path.substringAfterLast(".", "flac").uppercase()
            val size = currentTrack.fileSizeFormatted
            "$extension  |  $size  |  44.1 KHZ"
                currentTrack.path.endsWith(".flac", true) -> "FLAC"
                currentTrack.path.endsWith(".mp3", true) -> "MP3"
                currentTrack.path.endsWith(".m4a", true) || currentTrack.path.endsWith(".aac", true) -> "AAC"
                currentTrack.path.endsWith(".wav", true) -> "WAV"
                currentTrack.path.endsWith(".opus", true) -> "OPUS"
                currentTrack.path.endsWith(".ogg", true) -> "OGG"
                else -> "FLAC"
            }
            "$extension  ${currentTrack.bitrate.uppercase()}  ${currentTrack.sampleRate.uppercase()}"
        }
    }"""

new_block = """    val formatTag = remember(currentTrack) {
        if (currentTrack == null) "FLAC  988 KBPS  44.1 KHZ"
        else {
            val extension = when {
                currentTrack.path.endsWith(".flac", true) -> "FLAC"
                currentTrack.path.endsWith(".mp3", true) -> "MP3"
                currentTrack.path.endsWith(".m4a", true) || currentTrack.path.endsWith(".aac", true) -> "AAC"
                currentTrack.path.endsWith(".wav", true) -> "WAV"
                currentTrack.path.endsWith(".opus", true) -> "OPUS"
                currentTrack.path.endsWith(".ogg", true) -> "OGG"
                else -> "FLAC"
            }
            // Use fallback strings if bitrate/samplerate properties don't exist
            "$extension  |  ${currentTrack.fileSizeFormatted}  |  44.1 KHZ"
        }
    }"""

content = content.replace(old_block, new_block)
with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
