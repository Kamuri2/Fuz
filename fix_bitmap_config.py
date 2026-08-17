import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Replace ARGB_8888 with dynamic config based on API version
    replacement = """inPreferredConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    android.graphics.Bitmap.Config.HARDWARE
                                } else {
                                    android.graphics.Bitmap.Config.ARGB_8888
                                }"""
                                
    # For PlaybackService.kt
    content = content.replace("inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888", replacement)
    
    with open(filepath, 'w') as f:
        f.write(content)

replace_in_file("app/src/main/java/com/example/player/PlaybackService.kt")
replace_in_file("app/src/main/java/com/example/ui/components/TrackImage.kt")
