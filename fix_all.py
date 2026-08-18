import re

with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

start_idx = content.find("        var albumArtBitmap: android.graphics.Bitmap? = null\n        try {")
end_idx = content.find("        } catch (e: Exception) {", start_idx)

if start_idx != -1 and end_idx != -1:
    new_block = """        var albumArtBitmap: android.graphics.Bitmap? = null
        try {
            val mmr = android.media.MediaMetadataRetriever()
            if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                try {
                    mmr.setDataSource(track.path)
                } catch (e:Exception) {
                    mmr.setDataSource(this, track.contentUri)
                }
            } else {
                mmr.setDataSource(this, track.contentUri)
            }
            val rawBytes = mmr.embeddedPicture
            mmr.release()
            if (rawBytes != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                    albumArtBitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.setTargetSampleSize(2)
                    }
                } else {
                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                    albumArtBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                }
            }
"""
    content = content[:start_idx] + new_block + content[end_idx:]
    with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
        f.write(content)

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    lines = f.readlines()

out = []
in_block = False
brace_count = 0

for line in lines:
    if "LaunchedEffect(track.id) {" in line:
        in_block = True
        out.append(line)
        out.append("""        if (bitmap == null && !useFallback) {
            withContext(Dispatchers.IO) {
                try {
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                            try {
                                mmr.setDataSource(track.path)
                            } catch (e: Exception) {
                                mmr.setDataSource(context, track.contentUri)
                            }
                        } else {
                            mmr.setDataSource(context, track.contentUri)
                        }
                        
                        val rawBytes = mmr.embeddedPicture
                        if (rawBytes != null) {
                            val decoded: android.graphics.Bitmap?
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                                decoded = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                    var sampleSize = 1
                                    if (info.size.height > 800 || info.size.width > 800) {
                                        var halfHeight = info.size.height / 2
                                        var halfWidth = info.size.width / 2
                                        while (halfHeight / sampleSize >= 800 && halfWidth / sampleSize >= 800) {
                                            sampleSize *= 2
                                        }
                                    }
                                    decoder.setTargetSampleSize(sampleSize)
                                }
                            } else {
                                val options = android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    
                                var inSampleSize = 1
                                if (options.outHeight > 800 || options.outWidth > 800) {
                                    val halfHeight = options.outHeight / 2
                                    val halfWidth = options.outWidth / 2
                                    while (halfHeight / inSampleSize >= 800 && halfWidth / inSampleSize >= 800) {
                                        inSampleSize *= 2
                                    }
                                }

                                val finalOptions = android.graphics.BitmapFactory.Options().apply {
                                    this.inSampleSize = inSampleSize
                                    inPreferredConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        android.graphics.Bitmap.Config.HARDWARE
                                    } else {
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    }
                                }
                                decoded = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)
                            }
                            if (decoded != null) {
                                ArtworkCache.cache.put(track.path, decoded)
                                bitmap = decoded
                                return@withContext
                            }
                        }
                    } finally {
                        mmr.release()
                    }
                } catch (e: Exception) {}
                
                useFallback = true
            }
        }
    }
""")
        continue
        
    if in_block:
        if "if (bitmap != null) {" in line:
            in_block = False
            out.append(line)
        continue
        
    out.append(line)

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.writelines(out)
