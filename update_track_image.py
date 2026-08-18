import re

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    content = f.read()

new_logic = """                        var rawBytes = mmr.embeddedPicture
                        
                        // Si falla o es nulo (común en OGG/OPUS), intentamos con JAudioTagger
                        if (rawBytes == null) {
                            try {
                                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(java.io.File(track.path))
                                val tag = audioFile.tag
                                val artwork = tag?.firstArtwork
                                if (artwork != null) {
                                    rawBytes = artwork.binaryData
                                }
                            } catch (e: Exception) {
                                // Ignorar
                            }
                        }

                        if (rawBytes != null) {"""

content = content.replace("                        val rawBytes = mmr.embeddedPicture\n                        if (rawBytes != null) {", new_logic)

# Remove albumArtUri fallback
content = content.replace("} else if (useFallback && track.albumArtUri != null) {\n        AsyncImage(\n            model = track.albumArtUri,\n            contentDescription = track.title,\n            contentScale = contentScale,\n            modifier = modifier\n        )\n    }", "}")

# Make sure to import JAudioTagger in TrackImage.kt if not present (it uses fully qualified name)

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)

