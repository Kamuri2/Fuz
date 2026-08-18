with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    lines = f.readlines()

out = []
in_block = False
for l in lines:
    if 'val mmr = android.media.MediaMetadataRetriever()' in l:
        in_block = True
        continue
    
    if 'if (track.path.isNotBlank() && !track.path.startsWith("content://")) {' in l and 'mmr' not in l:
        in_block = True
        continue
        
    if in_block and '} finally {' in l:
        in_block = False

# We'll just replace the whole body
