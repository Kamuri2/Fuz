with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

old = """                    val isGenericName = id == "COMM" || id == "TXXX" || id == "TEXT"
                    val looksLikeLyrics = hasSyncedTimestamps || (content.lines().size > 4)

                    if (isLyricField || hasSyncedTimestamps) {
                        if (isGenericName && !looksLikeLyrics) {
                            continue
                        }"""

new = """                    val isGenericName = id == "COMM" || id == "TXXX" || id == "TEXT"
                    // Accept if it has timestamps, or if it has multiple lines, or if it is a reasonable length to be a small lyric
                    val looksLikeLyrics = hasSyncedTimestamps || content.lines().size >= 2 || content.length > 40

                    if (isLyricField || hasSyncedTimestamps || looksLikeLyrics) {
                        // Skip known ripper/encoder tags
                        val lower = content.lowercase()
                        if (isGenericName && (lower.contains("ripped by") || lower.contains("encoded by") || lower.contains("lame") || lower.contains("lavf") || !looksLikeLyrics)) {
                            continue
                        }"""

content = content.replace(old, new)
with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)
