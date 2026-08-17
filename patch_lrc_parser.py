with open('app/src/main/java/com/example/data/LrcParser.kt', 'r') as f:
    content = f.read()

new_lrc_parser = """package com.example.data

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    // Matches [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx], [m:ss.xx]
    private val timeTagRegex = Regex(\"\"\"\\[(\\d{2,}):(\\d{2})(?:[.:](\\d{2,3}))?\\]\"\"\")
    private val offsetRegex = Regex(\"\"\"\\[offset:([+-]?\\d+)\\]\"\"\", RegexOption.IGNORE_CASE)
    private val enhancedLrcRegex = Regex(\"\"\"<\\d{2,}:\\d{2}[.:]\\d{2,3}>\"\"\")

    fun parse(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        var globalOffset = 0L

        // Find offset first if it exists
        val offsetMatch = offsetRegex.find(lrcContent)
        if (offsetMatch != null) {
            globalOffset = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        lrcContent.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach
            // Ignore standard tags
            if (line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:") || line.startsWith("[by:") || line.startsWith("[offset:") || line.startsWith("[re:") || line.startsWith("[ve:")) return@forEach

            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                // Strip all timestamp tags and enhanced tags to get the pure lyric text
                var text = line.replace(timeTagRegex, "")
                text = text.replace(enhancedLrcRegex, "").trim()
                
                if (text.isNotEmpty()) {
                    for (match in matches) {
                        val mins = match.groupValues[1].toLongOrNull() ?: 0L
                        val secs = match.groupValues[2].toLongOrNull() ?: 0L
                        val fractionStr = match.groupValues.getOrNull(3) ?: ""

                        val millis = when (fractionStr.length) {
                            1 -> (fractionStr.toLongOrNull() ?: 0L) * 100
                            2 -> (fractionStr.toLongOrNull() ?: 0L) * 10
                            3 -> fractionStr.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        val totalMs = (mins * 60 * 1000) + (secs * 1000) + millis + globalOffset
                        
                        // Avoid negative times due to negative offset
                        val finalTimeMs = if (totalMs < 0) 0L else totalMs
                        lines.add(LrcLine(finalTimeMs, text))
                    }
                }
            }
        }

        return lines.sortedBy { it.timeMs }
    }

    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        return lines.indexOfLast { it.timeMs <= positionMs }
    }
}"""

with open('app/src/main/java/com/example/data/LrcParser.kt', 'w') as f:
    f.write(new_lrc_parser)
