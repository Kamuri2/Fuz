import os
import re

def process_file(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, 'r') as f: content = f.read()
    
    # replace any remaining block that initializes artworkBitmap with remember(*albumArtBytes*) ...
    content = re.sub(r'val artworkBitmap = remember\([^\)]*albumArtBytes[^\)]*\)\s*\{[^}]*albumArtBytes\?\.let\s*\{[^\}]*\}[^\}]*\}', 'val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null', content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f: f.write(content)

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
for f in files_to_clean: process_file(f)
