import re

with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'r') as f:
    content = f.read()

clean_artist_old = """    private fun cleanArtistName(raw: String): String {
        var a = raw.trim()
        if (isUnknownArtist(a)) return ""
        // Remove "feat. ..." or "ft. ..."
        a = a.replace(Regex("(?i)\\\\s+(?:feat\\\\.|ft\\\\.|featuring)\\\\s+.*$"), "")
        return a.trim()
    }"""

clean_artist_new = """    private fun cleanArtistName(raw: String): String {
        var a = raw.trim()
        if (isUnknownArtist(a)) return ""
        // Remove "feat. ..." or "ft. ..."
        a = a.replace(Regex("(?i)\\\\s+(?:feat\\\\.|ft\\\\.|featuring)\\\\s+.*$"), "")
        // Keep only the primary artist for better search match (split by &, ,, or ' and ')
        val split = a.split(Regex("[,&]|\\\\band\\\\b", RegexOption.IGNORE_CASE))
        if (split.isNotEmpty()) {
            a = split[0]
        }
        return a.trim()
    }"""

content = content.replace(clean_artist_old, clean_artist_new)

with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'w') as f:
    f.write(content)
