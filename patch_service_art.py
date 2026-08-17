import re

with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

old_art = """        var albumArtBitmap: android.graphics.Bitmap? = null
        if (track.albumArtUri != null) {
            try {
                albumArtBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(contentResolver, track.albumArtUri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, track.albumArtUri)
                }
            } catch (e: Exception) {}
        }"""

new_art = """        var albumArtBitmap: android.graphics.Bitmap? = null
        try {
            if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                val file = java.io.File(track.path)
                if (file.exists()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val rawBytes = audioFile.tag?.firstArtwork?.binaryData
                    if (rawBytes != null) {
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                        
                        // Downsample for notification to save memory and avoid TransactionTooLargeException
                        var inSampleSize = 1
                        if (options.outHeight > 300 || options.outWidth > 300) {
                            val halfHeight = options.outHeight / 2
                            val halfWidth = options.outWidth / 2
                            while (halfHeight / inSampleSize >= 300 && halfWidth / inSampleSize >= 300) {
                                inSampleSize *= 2
                            }
                        }
                        val finalOptions = android.graphics.BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        }
                        albumArtBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)
                    }
                }
            }
        } catch (e: Exception) {}
        
        if (albumArtBitmap == null && track.albumArtUri != null) {
            try {
                // For MediaStore URIs, we should also try to limit size if possible, but keeping it simple for now
                val pfd = contentResolver.openFileDescriptor(track.albumArtUri, "r")
                if (pfd != null) {
                    val fd = pfd.fileDescriptor
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeFileDescriptor(fd, null, options)
                    var inSampleSize = 1
                    if (options.outHeight > 300 || options.outWidth > 300) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / inSampleSize >= 300 && halfWidth / inSampleSize >= 300) {
                            inSampleSize *= 2
                        }
                    }
                    val finalOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    albumArtBitmap = android.graphics.BitmapFactory.decodeFileDescriptor(fd, null, finalOptions)
                    pfd.close()
                }
            } catch (e: Exception) {}
        }"""

content = content.replace(old_art, new_art)

with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
    f.write(content)
