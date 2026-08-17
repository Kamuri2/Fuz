import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

old_keys = """        val candidateKeys = listOf(
            "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS"
        )"""

new_keys = """        val candidateKeys = listOf(
            "SYLT", "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS"
        )"""

content = content.replace(old_keys, new_keys)

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)
