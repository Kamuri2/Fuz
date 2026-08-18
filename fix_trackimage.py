with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    content = f.read()

import re
content = content.replace("import org.jaudiotagger.audio.AudioFileIO\n", "")

old = """                        if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                            val file = File(track.path)
                            if (file.exists()) {
                                val audioFile = AudioFileIO.read(file)
                                val tag = audioFile.tag
                                val rawBytes = tag?.firstArtwork?.binaryData
                                if (rawBytes != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                                        val bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                            decoder.setTargetSampleSize(2)
                                        }
                                        artworkBitmap = bmp.asImageBitmap()
                                        ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                        return@withContext
                                    } else {
                                        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                                        val bmp = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                        if (bmp != null) {
                                            artworkBitmap = bmp.asImageBitmap()
                                            ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                            return@withContext
                                        }
                                    }
                                }
                            }
                        }
                        
                        val mmr = android.media.MediaMetadataRetriever()
                        try {
                            mmr.setDataSource(context, track.contentUri)
                            val rawBytes = mmr.embeddedPicture
                            if (rawBytes != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                                    val bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                        decoder.setTargetSampleSize(2)
                                    }
                                    artworkBitmap = bmp.asImageBitmap()
                                    ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                } else {
                                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    if (bmp != null) {
                                        artworkBitmap = bmp.asImageBitmap()
                                        ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                    }
                                }
                            }
                        } finally {
                            mmr.release()
                        }"""

new = """                        val mmr = android.media.MediaMetadataRetriever()
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                                    val bmp = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                        decoder.setTargetSampleSize(2)
                                    }
                                    artworkBitmap = bmp.asImageBitmap()
                                    ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                } else {
                                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                                    if (bmp != null) {
                                        artworkBitmap = bmp.asImageBitmap()
                                        ArtworkCache.put(track.id.toString(), artworkBitmap!!)
                                    }
                                }
                            }
                        } finally {
                            mmr.release()
                        }"""

content = content.replace(old, new)
with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)
