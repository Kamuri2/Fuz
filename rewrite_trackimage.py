import re

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    content = f.read()

# remove JAudioTagger
content = content.replace("import org.jaudiotagger.audio.AudioFileIO\n", "")

old = """                    if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                        val file = File(track.path)
                        if (file.exists()) {
                            val audioFile = AudioFileIO.read(file)
                            val rawBytes = audioFile.tag?.firstArtwork?.binaryData"""

new = """                    var rawBytes: ByteArray? = null
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                            try {
                                mmr.setDataSource(track.path)
                            } catch (e: Exception) {
                                mmr.setDataSource(context, track.contentUri)
                            }
                        } else {
                            mmr.setDataSource(context, track.contentUri)
                        }
                        rawBytes = mmr.embeddedPicture
                    } finally {
                        mmr.release()
                    }
                    
                    if (rawBytes != null) {"""

content = content.replace(old, new)
content = content.replace("                            } // end file.exists\n", "")
content = content.replace("                        } // end if content://\n", "")
content = content.replace("                        }\n                    }\n                    \n                    val mmr =", "                        }\n                    }\n/*")
content = content.replace("                        } finally {\n                            mmr.release()\n                        }\n                    }", "*/\n                    }")


with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)
