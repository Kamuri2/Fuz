with open('app/src/main/java/com/example/player/PlaybackService.kt', 'r') as f:
    content = f.read()

import re

old = """        try {
            if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                val file = java.io.File(track.path)
                if (file.exists()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val rawBytes = audioFile.tag?.firstArtwork?.binaryData
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
                }
            } else {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(this, track.contentUri)
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }"""

new = """        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }"""

content = content.replace(old, new)
with open('app/src/main/java/com/example/player/PlaybackService.kt', 'w') as f:
    f.write(content)
