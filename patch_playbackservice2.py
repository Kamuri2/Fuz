import re

with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

old_code = """                val pfd = contentResolver.openFileDescriptor(track.albumArtUri, "r")
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
                }"""

new_code = """                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, track.albumArtUri)
                    albumArtBitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        var sampleSize = 1
                        if (info.size.height > 300 || info.size.width > 300) {
                            var halfHeight = info.size.height / 2
                            var halfWidth = info.size.width / 2
                            while (halfHeight / sampleSize >= 300 && halfWidth / sampleSize >= 300) {
                                sampleSize *= 2
                            }
                        }
                        decoder.setTargetSampleSize(sampleSize)
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
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
                }"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
    f.write(content)
