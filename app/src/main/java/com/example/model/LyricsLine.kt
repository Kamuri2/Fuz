package com.example.model

data class LyricsLine(
    val timestampMs: Long,
    val text: String
) {
    fun formatTime(): String {
        val totalSecs = timestampMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}

object LyricsParser {
    /**
     * Parses LRC format string e.g. "[00:12.50]Line of lyrics text"
     */
    fun parseLrc(lrcText: String): List<LyricsLine> {
        if (lrcText.isBlank()) return emptyList()
        val lines = mutableListOf<LyricsLine>()
        // Permissive regex for formats like [mm:ss], [m:ss.ms], <mm:ss.ms>, [mm:ss:ms]
        val lrcRegex = Regex("(?:\\[|<)(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?(?:\\]|>)(.*)")

        lrcText.lines().forEach { line ->
            val cleanLine = line.trim()
            val match = lrcRegex.find(cleanLine)
            if (match != null) {
                val minsStr = match.groupValues[1]
                val secsStr = match.groupValues[2]
                val msStr = match.groupValues[3]
                val content = match.groupValues[4]
                
                val mins = minsStr.toLongOrNull() ?: 0L
                val secs = secsStr.toLongOrNull() ?: 0L
                val msFactor = if (msStr.length == 2) 10L else if (msStr.length == 1) 100L else 1L
                val ms = if (msStr.isNotEmpty()) (msStr.toLongOrNull() ?: 0L) * msFactor else 0L
                val timestamp = mins * 60_000L + secs * 1_000L + ms
                
                if (content.isNotBlank()) {
                    lines.add(LyricsLine(timestamp, content.trim()))
                }
            } else if (cleanLine.isNotBlank() && !cleanLine.startsWith("[") && !cleanLine.startsWith("<")) {
                // Plain unsynced text line without timestamps
                lines.add(LyricsLine(0L, cleanLine))
            } else if (cleanLine.isNotBlank()) {
                // If it starts with [ but didn't match the regex (e.g. [Chorus]), just treat it as text
                lines.add(LyricsLine(0L, cleanLine.replace(Regex("(?:\\[|<).*?(?:\\]|>)"), "").trim()))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
