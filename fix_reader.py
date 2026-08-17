import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

# Make field matching STRCIT again so we don't grab comments or random TXXX
old_match = """val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("TEXT") || id.contains("LYR") ||
                                       id.contains("TXXX") || id.contains("COMM")"""
new_match = """val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("©LYR") || id.contains("UNSYNCED")"""
content = content.replace(old_match, new_match)

old_cand = """val candidateKeys = listOf(
            "SYLT", "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS",
            "lyrics", "unsynced lyrics", "synced lyrics", "SYLT:Lyrics", "USLT:Lyrics",
            "COMM", "TXXX"
        )"""

new_cand = """val candidateKeys = listOf(
            "SYLT", "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS"
        )"""
content = content.replace(old_cand, new_cand)

# Also fix the convert subtitle fallback assignment. We should only overwrite fallbackUnsynced if it's ACTUALLY lyrics.
# Since we tightened isLyricField, it's safer now.

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)

