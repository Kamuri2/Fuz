import re
import glob

files = glob.glob('app/src/main/java/com/example/ui/screens/*.kt')

# In Compose, AsyncImage resolves Uris. MediaStore content URIs usually resolve to the actual image.
# However, if MediaStore URIs provide low quality images, it might be due to Coil resizing them too small automatically
# if they are in a small container, or due to Coil extracting thumbnails by default.
# Android 10+ uses `loadThumbnail` for content://media/... if size constraints are given, which can look pixelated.
# To fix this, we can tell Coil to not crossfade, or we can configure the ImageRequest to not be memory-limited if needed.
# But more likely, it's about the MetadataReader providing a low-res image.
# Let's check `MetadataReader.kt` where the album art is fetched.
