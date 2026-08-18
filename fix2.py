import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    c = f.read()

old_match = r"""val isLyricField = id\.contains\("LYRIC"\) \|\| id\.contains\("LRC"\) \|\| id\.contains\("SYLT"\) \|\|\s*id\.contains\("USLT"\) \|\| id\.contains\("©LYR"\) \|\| id\.contains\("UNSYNCED"\)\s*val hasSyncedTimestamps = content\.contains\(Regex\("\(\\\\\\[\|<\)\\\\d\{1,3\}:\\\\d\{1,2\}"\)\)\s*if \(isLyricField \|\| hasSyncedTimestamps\) \{"""

new_match = """val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("©LYR") || id.contains("UNSYNCED") ||
                                       id.contains("TEXT") || id.contains("TXXX") || id.contains("COMM")

                    val hasSyncedTimestamps = content.contains(Regex("(\\\\[|<)\\\\d{1,3}:\\\\d{1,2}"))
                    
                    val isGenericName = id == "COMM" || id == "TXXX" || id == "TEXT"
                    val looksLikeLyrics = hasSyncedTimestamps || (content.lines().size > 4)

                    if (isLyricField || hasSyncedTimestamps) {
                        if (isGenericName && !looksLikeLyrics) {
                            continue
                        }"""

c = re.sub(old_match, new_match, c, count=1)

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(c)
