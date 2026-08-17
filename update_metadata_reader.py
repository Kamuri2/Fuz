import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

# 1. searchSidecarLyrics update to support .srt and .vtt
sidecar_search = """                    if ((fBase == nameWithoutExt || fName == "$nameWithoutExt.lrc" || fName == "$nameWithoutExt.txt") &&
                        (fName.endsWith(".lrc") || fName.endsWith(".txt"))) {
                        val text = readFileWithCharsetDetection(f)
                        if (text.isNotBlank()) return text.trim()
                    }"""

sidecar_replace = """                    if ((fBase == nameWithoutExt || fName == "$nameWithoutExt.lrc" || fName == "$nameWithoutExt.txt" || fName == "$nameWithoutExt.srt" || fName == "$nameWithoutExt.vtt") &&
                        (fName.endsWith(".lrc") || fName.endsWith(".txt") || fName.endsWith(".srt") || fName.endsWith(".vtt"))) {
                        val text = readFileWithCharsetDetection(f)
                        if (text.isNotBlank()) {
                            return convertSubtitleToLrc(text.trim())
                        }
                    }"""

content = content.replace(sidecar_search, sidecar_replace)

# 2. Also convert ID3 tags text in case they stored SRT inside the LYRICS field
extract_lyrics_search = """        // 2. Extract lyrics
        var extractedLyrics = ""
"""
extract_lyrics_replace = """        // 2. Extract lyrics
        var extractedLyrics = ""
"""

tag_extract_search = """                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                            return clean // Synchronized!
                        }"""
tag_extract_replace = """                        val converted = convertSubtitleToLrc(clean)
                        if (converted.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                            return converted // Synchronized!
                        }"""
content = content.replace(tag_extract_search, tag_extract_replace)

field_extract_search = """                            if (hasSyncedTimestamps) {
                                return cleaned // Return immediately if synchronized
                            }"""
field_extract_replace = """                            val converted = convertSubtitleToLrc(cleaned)
                            if (converted.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                                return converted
                            }"""
content = content.replace(field_extract_search, field_extract_replace)

cand_extract_search = """                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) return clean"""
cand_extract_replace = """                        val converted = convertSubtitleToLrc(clean)
                        if (converted.contains(Regex("\\[\\d{1,2}:\\d{2}"))) return converted"""
content = content.replace(cand_extract_search, cand_extract_replace)

# 3. Add convertSubtitleToLrc function
convert_func = """
    private fun convertSubtitleToLrc(content: String): String {
        if (content.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
            return content
        }
        
        val builder = StringBuilder()
        val lines = content.lines()
        val timeRegex = Regex("(?:\\d{2}:)?(\\d{2}):(\\d{2})[,.](\\d{2,3})\\s*-->.*")
        var currentTimestamp = ""
        
        for (line in lines) {
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1]
                val sec = match.groupValues[2]
                var milli = match.groupValues[3]
                if (milli.length == 3) milli = milli.substring(0, 2)
                currentTimestamp = "[$min:$sec.$milli]"
            } else if (line.isNotBlank() && !line.matches(Regex("^\\d+$")) && !line.startsWith("WEBVTT")) {
                if (currentTimestamp.isNotBlank()) {
                    builder.append(currentTimestamp).append(line).append("\\n")
                    currentTimestamp = ""
                }
            }
        }
        
        if (builder.isNotEmpty()) {
            return builder.toString().trim()
        }
        
        return content
    }
"""

content = content + "\n" + convert_func

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)

