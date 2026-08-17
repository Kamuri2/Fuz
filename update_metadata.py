import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

# Make regex for synced lyrics more permissive
content = content.replace('Regex("\\\\[\\\\d{1,2}:\\\\d{2}")', 'Regex("(\\\\[|<)\\\\d{1,3}:\\\\d{1,2}")')
content = content.replace('Regex("\\[\\\\d{1,2}:\\\\d{2}")', 'Regex("(\\\\[|<)\\\\d{1,3}:\\\\d{1,2}")')

# Make field matching more permissive
old_match = """val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("TEXT") || id.contains("©LYR") ||
                                       id.contains("TXXX") || id.contains("COMM")"""
new_match = """val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("TEXT") || id.contains("LYR") ||
                                       id.contains("TXXX") || id.contains("COMM")"""
content = content.replace(old_match, new_match)

# Make candidate keys exhaustive
old_cand = """val candidateKeys = listOf(
            "SYLT", "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS"
        )"""

new_cand = """val candidateKeys = listOf(
            "SYLT", "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS",
            "lyrics", "unsynced lyrics", "synced lyrics", "SYLT:Lyrics", "USLT:Lyrics",
            "COMM", "TXXX"
        )"""
content = content.replace(old_cand, new_cand)

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)

