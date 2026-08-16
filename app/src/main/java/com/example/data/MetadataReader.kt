package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.model.Track
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger

object MetadataReader {

    private const val TAG = "MetadataReader"

    /**
     * Reads complete metadata from an audio Uri using MediaMetadataRetriever
     * and deep JAudioTagger extraction for embedded LRC lyrics and tags across all formats (MP3, FLAC, WAV, M4A, OGG, OPUS).
     */
    fun extractFullMetadata(context: Context, track: Track): Track {
        val retriever = MediaMetadataRetriever()
        var tempCacheFile: File? = null
        return try {
            try {
                retriever.setDataSource(context, track.contentUri)
            } catch (e: Exception) {
                Log.d(TAG, "MediaMetadataRetriever setDataSource error: ${e.message}")
            }

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: track.title
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: track.artist
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: track.album
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.toIntOrNull() ?: track.year
            
            val trackNumStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            var trackNumber = parseTrackNumber(trackNumStr) ?: track.trackNumber
            
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() } ?: track.genre
            val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bitrate = bitrateRaw?.toLongOrNull()?.let { "${it / 1000} kbps" } ?: track.bitrate
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: track.durationMs
            
            var sampleRate = track.sampleRate
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, track.contentUri, null)
                if (extractor.trackCount > 0) {
                    val format = extractor.getTrackFormat(0)
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        sampleRate = "${sr / 1000.0} kHz".replace(".0 kHz", " kHz")
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                Log.d(TAG, "Extractor error: ${e.message}")
            }

            var artworkBytes = retriever.embeddedPicture
            var extractedLyrics = ""

            // 1. Extracción profunda con JAudioTagger (MP3, FLAC, M4A, OGG, OPUS, WAV, AAC)
            try {
                Logger.getLogger("org.jaudiotagger").level = Level.OFF
                var targetFile: File? = null

                if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                    val directFile = File(track.path)
                    if (directFile.exists() && directFile.canRead()) {
                        targetFile = directFile
                    }
                }

                // Si no hay archivo directo accesible, crear una copia temporal para leer con JAudioTagger
                if (targetFile == null) {
                    try {
                        context.contentResolver.openInputStream(track.contentUri)?.use { input ->
                            val suffix = getExtensionFromTrack(track)
                            val cacheFile = File.createTempFile("tag_scan_", suffix, context.cacheDir)
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                            tempCacheFile = cacheFile
                            targetFile = cacheFile
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Cache file copy error: ${e.message}")
                    }
                }

                if (targetFile != null && targetFile.exists()) {
                    val audioFile = AudioFileIO.read(targetFile)
                    val tag = audioFile.tag
                    
                    if (tag != null) {
                        // Extraer número de pista de los metadatos si no se obtuvo antes
                        val tagTrack = tag.getFirst(FieldKey.TRACK)
                        if (!tagTrack.isNullOrBlank()) {
                            parseTrackNumber(tagTrack)?.let {
                                if (it > 0) trackNumber = it
                            }
                        }

                        // Buscar letras sincronizadas o incrustadas en todos los formatos conocidos
                        extractedLyrics = findLyricsInTag(tag)
                        
                        // Extraer carátula si MediaMetadataRetriever falló
                        if (artworkBytes == null) {
                            val artwork = tag.firstArtwork
                            if (artwork != null && artwork.binaryData != null) {
                                artworkBytes = artwork.binaryData
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "JAudioTagger extraction note: ${e.message}")
            }

            // 2. Intento nativo de Android como respaldo (MediaMetadataRetriever)
            if (extractedLyrics.isBlank()) {
                try {
                    val rawLyrics = retriever.extractMetadata(1000) // METADATA_KEY_LYRICS
                    if (!rawLyrics.isNullOrBlank()) {
                        extractedLyrics = cleanExtractedLyrics(rawLyrics)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No embedded lyrics found via MediaMetadataRetriever")
                }
            }

            // 3. Buscar archivos sueltos (.lrc / .txt) en la misma carpeta
            if (extractedLyrics.isBlank() && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                extractedLyrics = searchSidecarLyrics(track.path)
            }

            // 4. Fallback a API de Internet (LrcLib) si no hay letras locales
            if (extractedLyrics.isBlank()) {
                val fetched = LrcLibHelper.fetchLyrics(title, artist)
                if (fetched != null) {
                    extractedLyrics = fetched
                }
            }

            val finalLyrics = if (extractedLyrics.isNotBlank()) extractedLyrics else track.lyrics

            track.copy(
                title = title,
                artist = artist,
                album = album,
                year = year,
                trackNumber = trackNumber,
                genre = genre,
                bitrate = bitrate,
                sampleRate = sampleRate,
                durationMs = durationMs,
                albumArtBytes = artworkBytes,
                lyrics = finalLyrics
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata for ${track.title}: ${e.message}")
            track
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
            try {
                tempCacheFile?.delete()
            } catch (e: Exception) {}
        }
    }

    private fun parseTrackNumber(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return try {
            val cleaned = raw.trim()
            val firstPart = cleaned.split("/").firstOrNull()?.trim() ?: cleaned
            val num = firstPart.toIntOrNull()
            if (num != null && num >= 1000) {
                num % 1000
            } else {
                num
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findLyricsInTag(tag: Tag): String {
        // Lista exhaustiva de claves comunes en MP3 (ID3), FLAC (Vorbis), M4A (MP4), OGG, OPUS, WAV
        val candidateKeys = listOf(
            "SYNCEDLYRICS",
            "SYNCED LYRICS",
            "TXXX:SYNCEDLYRICS",
            "TXXX:SYNCED LYRICS",
            "TXXX:LRC",
            "SYLT",
            "UNSYNCEDLYRICS",
            "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS",
            "TXXX:UNSYNCED LYRICS",
            "TXXX:LYRICS",
            "TXXX:Lyrics",
            "LYRICS",
            "Lyrics",
            "©lyr",
            "TEXT",
            "COMM:Lyrics",
            "ULTRASTAR"
        )

        for (key in candidateKeys) {
            try {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    val cleaned = cleanExtractedLyrics(value)
                    if (cleaned.isNotBlank()) return cleaned
                }
            } catch (e: Exception) {}
        }

        // Intento con FieldKey estándar
        try {
            val stdLyrics = tag.getFirst(FieldKey.LYRICS)
            if (!stdLyrics.isNullOrBlank()) {
                val cleaned = cleanExtractedLyrics(stdLyrics)
                if (cleaned.isNotBlank()) return cleaned
            }
        } catch (e: Exception) {}

        return ""
    }

    private fun searchSidecarLyrics(audioPath: String): String {
        return try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) return ""
            val parent = audioFile.parentFile ?: return ""
            val nameWithoutExt = audioFile.nameWithoutExtension.lowercase()

            val files = parent.listFiles() ?: return ""
            for (f in files) {
                if (f.isFile) {
                    val fName = f.name.lowercase()
                    val fBase = f.nameWithoutExtension.lowercase()
                    if ((fBase == nameWithoutExt) && (fName.endsWith(".lrc") || fName.endsWith(".txt"))) {
                        val text = readFileWithCharsetDetection(f)
                        if (text.isNotBlank()) return text.trim()
                    }
                }
            }
            ""
        } catch (e: Exception) {
            Log.d(TAG, "Error searching sidecar lyrics: ${e.message}")
            ""
        }
    }

    private fun readFileWithCharsetDetection(file: File): String {
        return try {
            val bytes = file.readBytes()
            // Check UTF-8 BOM
            if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            } else if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            } else {
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            try {
                file.readText(Charsets.ISO_8859_1)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun getExtensionFromTrack(track: Track): String {
        val path = track.path.lowercase()
        return when {
            path.endsWith(".mp3") -> ".mp3"
            path.endsWith(".flac") -> ".flac"
            path.endsWith(".m4a") -> ".m4a"
            path.endsWith(".wav") -> ".wav"
            path.endsWith(".ogg") -> ".ogg"
            path.endsWith(".opus") -> ".opus"
            path.endsWith(".aac") -> ".aac"
            else -> ".mp3"
        }
    }

    /**
     * Limpia la metadata de letras incrustadas.
     * JAudioTagger a veces devuelve el frame con cabeceras de idioma u otros datos (ej. "eng||[00:12.00]Letra").
     */
    private fun cleanExtractedLyrics(raw: String): String {
        var cleaned = raw.trim()
        
        // Si el tag incrustado viene con separador de JAudioTagger (ej. "eng||[00:12.00]Letra")
        if (cleaned.contains("||")) {
            val parts = cleaned.split("||", limit = 2)
            if (parts.size == 2 && parts[0].length <= 6) {
                cleaned = parts[1]
            }
        }
        
        // Limpiar prefijos comunes de idioma que algunos editores incrustan
        val commonPrefixes = listOf("eng|", "spa|", "jpn|", "XXX|", "xxx|", "ger|", "fra|", "ita|", "zho|")
        for (prefix in commonPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length)
            }
        }

        // Eliminar caracteres nulos iniciales o finales si existían en los bytes binarios
        cleaned = cleaned.trim { it <= ' ' || it == '\u0000' }
        
        return cleaned
    }
}