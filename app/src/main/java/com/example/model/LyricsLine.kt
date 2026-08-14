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
        val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)")

        lrcText.lines().forEach { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val (minsStr, secsStr, msStr, content) = match.destructured
                val mins = minsStr.toLongOrNull() ?: 0L
                val secs = secsStr.toLongOrNull() ?: 0L
                val msFactor = if (msStr.length == 2) 10L else 1L
                val ms = (msStr.toLongOrNull() ?: 0L) * msFactor
                val timestamp = mins * 60_000L + secs * 1_000L + ms
                if (content.isNotBlank()) {
                    lines.add(LyricsLine(timestamp, content.trim()))
                }
            } else if (line.isNotBlank() && !line.startsWith("[")) {
                // Plain unsynced text line without timestamps
                lines.add(LyricsLine(0L, line.trim()))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
