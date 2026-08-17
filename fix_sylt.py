import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

imports = """import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
import org.jaudiotagger.tag.id3.AbstractID3v2Frame"""

content = content.replace("import org.jaudiotagger.tag.TagTextField", "import org.jaudiotagger.tag.TagTextField\n" + imports)

sylt_logic = """
        // 1.5. Check for SYLT (Synchronized Lyrics Text)
        try {
            if (tag is ID3v24Tag || tag is ID3v23Tag) {
                val abstractTag = tag as org.jaudiotagger.tag.id3.AbstractID3v2Tag
                if (abstractTag.hasFrame("SYLT")) {
                    val syltFrames = abstractTag.getFrame("SYLT")
                    if (syltFrames is List<*>) {
                        for (frame in syltFrames) {
                            if (frame is AbstractID3v2Frame) {
                                val body = frame.body
                                if (body is FrameBodySYLT) {
                                    val lyricsBytes = body.lyrics
                                    if (lyricsBytes != null && lyricsBytes.isNotEmpty()) {
                                        val parsedSylt = parseSyltToLrc(lyricsBytes)
                                        if (parsedSylt.isNotBlank()) {
                                            return parsedSylt
                                        }
                                    }
                                }
                            }
                        }
                    } else if (syltFrames is AbstractID3v2Frame) {
                        val body = syltFrames.body
                        if (body is FrameBodySYLT) {
                            val lyricsBytes = body.lyrics
                            if (lyricsBytes != null && lyricsBytes.isNotEmpty()) {
                                val parsedSylt = parseSyltToLrc(lyricsBytes)
                                if (parsedSylt.isNotBlank()) {
                                    return parsedSylt
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse SYLT: ${e.message}")
        }
"""

content = content.replace("        // 2. Iterate ALL fields in the tag", sylt_logic + "\n        // 2. Iterate ALL fields in the tag")

parse_sylt_func = """
    private fun parseSyltToLrc(lyricsBytes: ByteArray): String {
        try {
            val builder = java.lang.StringBuilder()
            var offset = 0
            while (offset < lyricsBytes.size) {
                // Find end of text string (null terminated)
                var textEnd = offset
                while (textEnd < lyricsBytes.size && lyricsBytes[textEnd].toInt() != 0) {
                    textEnd++
                }
                if (textEnd >= lyricsBytes.size) break
                
                val text = String(lyricsBytes, offset, textEnd - offset, Charsets.UTF_8).trim()
                
                // Timestamp is 4 bytes integer after the null terminator
                offset = textEnd + 1
                if (offset + 3 < lyricsBytes.size) {
                    val t1 = lyricsBytes[offset].toInt() and 0xFF
                    val t2 = lyricsBytes[offset + 1].toInt() and 0xFF
                    val t3 = lyricsBytes[offset + 2].toInt() and 0xFF
                    val t4 = lyricsBytes[offset + 3].toInt() and 0xFF
                    val timestampMs = (t1 shl 24) or (t2 shl 16) or (t3 shl 8) or t4
                    
                    val minutes = timestampMs / 60000
                    val seconds = (timestampMs % 60000) / 1000
                    val hundreths = (timestampMs % 1000) / 10
                    
                    val timeStr = String.format("[%02d:%02d.%02d]", minutes, seconds, hundreths)
                    if (text.isNotBlank()) {
                        builder.append(timeStr).append(text).append("\\n")
                    }
                    offset += 4
                } else {
                    break
                }
            }
            return builder.toString().trim()
        } catch (e: Exception) {
            return ""
        }
    }
"""

content = content + "\n" + parse_sylt_func

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)
