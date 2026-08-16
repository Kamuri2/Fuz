package com.example.data

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    // Matches [mm:ss], [m:ss], [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx]
    private val timeTagRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val metaTagRegex = Regex("\\[[a-zA-Z]{1,10}:[^\\]]*\\]")

    fun parse(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()

        lrcContent.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || metaTagRegex.matches(line)) return@forEach

            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isNotEmpty()) {
                // Strip all timestamp tags to get the pure lyric text
                val text = line.replace(timeTagRegex, "").trim()
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

                        val totalMs = (mins * 60 * 1000) + (secs * 1000) + millis
                        lines.add(LrcLine(totalMs, text))
                    }
                }
            }
        }

        return lines.sortedBy { it.timeMs }
    }

    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (positionMs >= lines[i].timeMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
