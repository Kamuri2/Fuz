package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.Track
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extrae el artwork a resolución original directamente del archivo.
 * NO usa MediaStore (que downscalea a 300-512px).
 * Mantiene caché en disco con clave hash/tamaño para 0 I/O en lecturas repetidas.
 */
class AlbumArtExtractor(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "album_art_hd")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Devuelve un File con la portada a máxima resolución disponible para un archivo de audio.
     * Si ya está cacheada, devuelve el archivo cacheado (0 IO).
     */
    fun getHighResArt(audioFile: File): File? {
        if (!audioFile.exists() || !audioFile.canRead()) return null

        val cacheFile = File(cacheDir, "${audioFile.nameWithoutExtension}_${audioFile.length()}.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

        return try {
            val bytes = extractOriginalArtworkBytes(audioFile) ?: return null

            cacheDir.mkdirs()
            cacheFile.writeBytes(bytes)
            cacheFile
        } catch (e: Exception) {
            Log.w("AlbumArtExtractor", "Fallo extrayendo artwork de ${audioFile.name}: ${e.message}")
            null
        }
    }

    /**
     * Versión para Track: si tiene ruta física lee el archivo, sino usa contentUri con MMR.
     */
    fun getHighResArt(track: Track): File? {
        if (track.path.isNotBlank()) {
            val file = File(track.path)
            if (file.exists() && file.canRead()) {
                val art = getHighResArt(file)
                if (art != null) return art
            }
        }

        // Intento con ContentUri si no hay ruta de archivo directa
        if (track.contentUri != Uri.EMPTY) {
            val cacheFile = File(cacheDir, "track_${track.id}_${track.durationMs}.jpg")
            if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

            return try {
                var bytes: ByteArray? = null
                val mmr = MediaMetadataRetriever()
                try {
                    context.contentResolver.openFileDescriptor(track.contentUri, "r")?.use { pfd ->
                        mmr.setDataSource(pfd.fileDescriptor)
                        bytes = mmr.embeddedPicture
                    }
                } finally {
                    try { mmr.release() } catch (ignored: Exception) {}
                }

                if (bytes != null && bytes!!.isNotEmpty()) {
                    cacheDir.mkdirs()
                    cacheFile.writeBytes(bytes!!)
                    cacheFile
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    /**
     * Versión para FLAC (el artwork va en PICTURE block, no en Vorbis Comments).
     */
    fun getFlacArt(audioFile: File): File? {
        val cacheFile = File(cacheDir, "${audioFile.nameWithoutExtension}_${audioFile.length()}.png")
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

        return try {
            val bytes = extractOriginalArtworkBytes(audioFile) ?: return null

            cacheDir.mkdirs()
            cacheFile.writeBytes(bytes)
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extrae los bytes puros sin downsamplear usando JAudioTagger -> MediaMetadataRetriever -> Sidecar images.
     */
    private fun extractOriginalArtworkBytes(audioFile: File): ByteArray? {
        // 1. JAudioTagger (mantiene resolución nativa de etiquetas ID3 APIC / FLAC PICTURE / MP4 covr)
        try {
            val audio = AudioFileIO.read(audioFile)
            val tag = audio.tag
            if (tag != null) {
                val pictures = tag.artworkList
                if (!pictures.isNullOrEmpty()) {
                    val best = pictures
                        .filter { it.binaryData != null && it.binaryData.isNotEmpty() }
                        .maxByOrNull { it.binaryData.size }
                    if (best?.binaryData != null && best.binaryData.isNotEmpty()) {
                        return best.binaryData
                    }
                }

                // Check Vorbis METADATA_BLOCK_PICTURE en FLAC/Ogg
                val picBase64 = tag.getFirst("METADATA_BLOCK_PICTURE")
                if (!picBase64.isNullOrBlank()) {
                    val decoded = decodeVorbisPictureBlock(picBase64)
                    if (decoded != null && decoded.isNotEmpty()) {
                        return decoded
                    }
                }

                // Check COVERART base64
                val coverBase64 = tag.getFirst("COVERART") ?: tag.getFirst("COVER_ART")
                if (!coverBase64.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(coverBase64.trim(), Base64.DEFAULT)
                        if (bytes.isNotEmpty()) return bytes
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.d("AlbumArtExtractor", "JAudioTagger no encontró arte embebido: ${e.message}")
        }

        // 2. MediaMetadataRetriever nativo a resolución completa
        try {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(audioFile.absolutePath)
                val rawBytes = mmr.embeddedPicture
                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    return rawBytes
                }
            } finally {
                try { mmr.release() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("AlbumArtExtractor", "MMR falló para ${audioFile.name}: ${e.message}")
        }

        // 3. Sidecar cover files en el mismo directorio (cover.jpg, folder.jpg, album.jpg, etc.)
        try {
            val parent = audioFile.parentFile
            if (parent != null && parent.exists() && parent.isDirectory) {
                val candidateNames = listOf(
                    "cover.jpg", "cover.png", "cover.webp", "cover.jpeg",
                    "folder.jpg", "folder.png", "folder.webp", "folder.jpeg",
                    "album.jpg", "album.png", "album.jpeg",
                    "${audioFile.nameWithoutExtension}.jpg",
                    "${audioFile.nameWithoutExtension}.png"
                )
                for (name in candidateNames) {
                    val sidecar = File(parent, name)
                    if (sidecar.exists() && sidecar.canRead() && sidecar.length() > 500) {
                        return sidecar.readBytes()
                    }
                }
            }
        } catch (ignored: Exception) {}

        return null
    }

    private fun decodeVorbisPictureBlock(base64: String): ByteArray? {
        return try {
            val raw = Base64.decode(base64.trim(), Base64.DEFAULT)
            if (raw.size < 32) return null
            val buf = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
            buf.getInt() // Picture type
            val mimeLen = buf.getInt()
            if (mimeLen < 0 || mimeLen > buf.remaining()) return null
            buf.position(buf.position() + mimeLen)
            val descLen = buf.getInt()
            if (descLen < 0 || descLen > buf.remaining()) return null
            buf.position(buf.position() + descLen)
            buf.getInt() // Width
            buf.getInt() // Height
            buf.getInt() // Color depth
            buf.getInt() // Colors used
            val dataLen = buf.getInt()
            if (dataLen <= 0 || dataLen > buf.remaining()) return null
            val imgBytes = ByteArray(dataLen)
            buf.get(imgBytes)
            imgBytes
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Limpieza periódica de artwork viejo en caché (elimina imágenes de más de 7 días).
     */
    fun purgeOldArt() {
        try {
            val cutoff = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
            cacheDir.listFiles()?.forEach {
                if (it.lastModified() < cutoff) it.delete()
            }
        } catch (e: Exception) {
            Log.w("AlbumArtExtractor", "Error purgando caché: ${e.message}")
        }
    }
}
