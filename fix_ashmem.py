import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Remove decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
    content = content.replace("decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE", "// decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE")
    
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file("app/src/main/java/com/example/player/PlaybackService.kt")
replace_in_file("app/src/main/java/com/example/ui/components/TrackImage.kt")
