import re

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    content = f.read()

old_code = """                                // Decode bounds first to prevent OOM and ashmem pinning issues
                                val options = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                
                                // Target size of 800x800 is high-res enough for the player
                                // but saves massive amounts of memory compared to raw 3000x3000 covers
                                var inSampleSize = 1
                                if (options.outHeight > 800 || options.outWidth > 800) {
                                    val halfHeight: Int = options.outHeight / 2
                                    val halfWidth: Int = options.outWidth / 2
                                    while (halfHeight / inSampleSize >= 800 && halfWidth / inSampleSize >= 800) {
                                        inSampleSize *= 2
                                    }
                                }

                                val finalOptions = BitmapFactory.Options().apply {
                                    this.inSampleSize = inSampleSize
                                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                    inMutable = false // Allows system to optimize memory (avoids ashmem pinning warnings)
                                }
                                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)"""

new_code = """                                val decoded: android.graphics.Bitmap?
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
                                        // Avoid hardware bitmaps in cache to prevent potential issues
                                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                                    }
                                } else {
                                    val options = BitmapFactory.Options().apply {
                                        inJustDecodeBounds = true
                                    }
                                    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    
                                    var inSampleSize = 1
                                    if (options.outHeight > 800 || options.outWidth > 800) {
                                        val halfHeight = options.outHeight / 2
                                        val halfWidth = options.outWidth / 2
                                        while (halfHeight / inSampleSize >= 800 && halfWidth / inSampleSize >= 800) {
                                            inSampleSize *= 2
                                        }
                                    }

                                    val finalOptions = BitmapFactory.Options().apply {
                                        this.inSampleSize = inSampleSize
                                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                                    }
                                    decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, finalOptions)
                                }"""

content = content.replace(old_code, new_code)
with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)
