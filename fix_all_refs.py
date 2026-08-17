import os
import re

def process_file(filepath, replacements):
    if not os.path.exists(filepath): return
    with open(filepath, 'r') as f: content = f.read()
    for old, new in replacements: content = content.replace(old, new)
    with open(filepath, 'w') as f: f.write(content)

# Remove artwork loading from variables that used albumArtBytes
files_to_clean = [
    'app/src/main/java/com/example/ui/screens/HomeScreen.kt',
    'app/src/main/java/com/example/ui/screens/AlbumsScreen.kt',
    'app/src/main/java/com/example/ui/screens/LibraryScreen.kt',
    'app/src/main/java/com/example/ui/screens/ArtistsScreen.kt',
    'app/src/main/java/com/example/ui/components/QueueBottomSheet.kt',
    'app/src/main/java/com/example/ui/components/LiquidVinylArtwork.kt',
    'app/src/main/java/com/example/ui/components/LiquidGlassBackground.kt',
    'app/src/main/java/com/example/MainActivity.kt'
]

for file in files_to_clean:
    process_file(file, [
        ('val artworkBitmap = remember(track.albumArtBytes) {\n        track.albumArtBytes?.let { bytes ->\n            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n        }\n    }', 'val artworkBitmap = null'),
        ('val artworkBitmap = remember(firstTrack?.albumArtBytes) {\n                            firstTrack?.albumArtBytes?.let { bytes ->\n                                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n                            }\n                        }', 'val artworkBitmap = null'),
        ('val artworkBitmap = remember(firstTrack?.albumArtBytes) {\n        firstTrack?.albumArtBytes?.let { bytes ->\n            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n        }\n    }', 'val artworkBitmap = null'),
        ('val artworkBitmap = remember(firstTrack?.albumArtBytes) {\n                    firstTrack?.albumArtBytes?.let { bytes ->\n                        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n                    }\n                }', 'val artworkBitmap = null'),
        ('val artworkBitmap = remember(track?.albumArtBytes) {\n        track?.albumArtBytes?.let { bytes ->\n            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n        }\n    }', 'val artworkBitmap = null'),
        ('val artworkBitmap = remember(currentTrack?.albumArtBytes) {\n        currentTrack?.albumArtBytes?.let { bytes ->\n            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }\n            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()\n        }\n    }', 'val artworkBitmap = null')
    ])

# specifically replace all remember(track.albumArtBytes) with generic null placeholders since we rely on AsyncImage / albumArtUri now
