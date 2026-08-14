package com.example.data

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    private val timeRegex = Regex("\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{2,3}))?\\]")

    fun parse(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LrcLine>()
        
        // Remove metadata tags like [ti:Title] [ar:Artist] etc before splitting
        val cleanedContent = lrcContent.replace(Regex("\\[[a-zA-Z]+:[^\\]]*\\]"), "")

        cleanedContent.lines().forEach { line ->
            val match = timeRegex.find(line)
            if (match != null) {
                val mins = match.groupValues[1].toLongOrNull() ?: 0L
                val secs = match.groupValues[2].toLongOrNull() ?: 0L
                val millisStr = if (match.groupValues.size > 3) match.groupValues[3] else ""
                
                val millis = if (millisStr.isNotEmpty()) {
                    if (millisStr.length == 2) millisStr.toLong() * 10 else millisStr.toLong()
                } else 0L
                
                val totalMs = (mins * 60 * 1000) + (secs * 1000) + millis
                
                // Remove all timestamps from the line to get just the text
                val text = line.replace(Regex("\\[\\d{2,}:\\d{2}(?:\\.\\d{2,3})?\\]"), "").trim()
                
                if (text.isNotEmpty()) {
                    lines.add(LrcLine(totalMs, text))
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
