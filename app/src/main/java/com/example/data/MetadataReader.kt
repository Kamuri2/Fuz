package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.Track
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagTextField
import java.io.File
import java.io.FileOutputStream
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

            var title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: track.title
            var artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: track.artist
            var album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
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
                            val suffix = getExtensionFromTrack(context, track)
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
                        // Title / Artist / Album fallback from tag if retriever was empty or unknown
                        val tagTitle = tag.getFirst(FieldKey.TITLE)
                        if (!tagTitle.isNullOrBlank() && (title.isBlank() || isGenericName(title))) {
                            title = tagTitle.trim()
                        }
                        val tagArtist = tag.getFirst(FieldKey.ARTIST)
                        if (!tagArtist.isNullOrBlank() && (artist.isBlank() || isGenericName(artist))) {
                            artist = tagArtist.trim()
                        }
                        val tagAlbum = tag.getFirst(FieldKey.ALBUM)
                        if (!tagAlbum.isNullOrBlank() && (album.isBlank() || isGenericName(album))) {
                            album = tagAlbum.trim()
                        }

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
                val fetched = LrcLibHelper.fetchLyrics(
                    title = title,
                    artist = artist,
                    album = album,
                    durationSec = durationMs / 1000
                )
                if (fetched != null) {
                    extractedLyrics = fetched
                }
            }

            val finalLyrics = if (extractedLyrics.isNotBlank()) extractedLyrics else track.lyrics

            track.copy(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                trackNumber = trackNumber,
                year = year,
                genre = genre,
                bitrate = bitrate,
                sampleRate = sampleRate,
                albumArtBytes = artworkBytes,
                lyrics = finalLyrics
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractFullMetadata: ${e.message}", e)
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

    private fun isGenericName(str: String): Boolean {
        val s = str.trim().lowercase()
        return s.isEmpty() || s == "unknown" || s == "unknown artist" || s == "unknown track" || s == "unknown album" || s == "<unknown>" || s == "artista desconocido"
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
        var fallbackUnsynced = ""

        // 1. Direct standard field check via FieldKey
        try {
            val stdList = tag.getAll(FieldKey.LYRICS)
            for (lyr in stdList) {
                if (!lyr.isNullOrBlank()) {
                    val clean = cleanExtractedLyrics(lyr)
                    if (clean.isNotBlank()) {
                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                            return clean // Synchronized!
                        }
                        if (fallbackUnsynced.isBlank()) {
                            fallbackUnsynced = clean
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        // 2. Iterate ALL fields in the tag
        try {
            val iterator = tag.fields
            while (iterator.hasNext()) {
                val field = iterator.next()
                val id = (field.id ?: "").uppercase()
                
                var content = ""
                if (field is TagTextField) {
                    content = field.content ?: ""
                }
                if (content.isBlank()) {
                    content = extractTextFromRawField(field.toString())
                }

                if (content.isNotBlank()) {
                    // Check if field is lyrics-related or contains timestamp lyrics
                    val isLyricField = id.contains("LYRIC") || id.contains("LRC") || id.contains("SYLT") || 
                                       id.contains("USLT") || id.contains("TEXT") || id.contains("©LYR") ||
                                       id.contains("TXXX") || id.contains("COMM")

                    val hasSyncedTimestamps = content.contains(Regex("\\[\\d{1,2}:\\d{2}"))

                    if (isLyricField || hasSyncedTimestamps) {
                        val cleaned = cleanExtractedLyrics(content)
                        if (cleaned.isNotBlank()) {
                            if (hasSyncedTimestamps) {
                                return cleaned // Return immediately if synchronized
                            }
                            if (fallbackUnsynced.isBlank()) {
                                fallbackUnsynced = cleaned
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error iterating fields: ${e.message}")
        }

        // 3. Fallback candidate keys lookup
        val candidateKeys = listOf(
            "SYNCEDLYRICS", "SYNCED LYRICS", "TXXX:SYNCEDLYRICS", "TXXX:SYNCED LYRICS",
            "TXXX:LRC", "LYRICS_SYNCED", "USLT", "UNSYNCEDLYRICS", "UNSYNCED LYRICS",
            "TXXX:UNSYNCEDLYRICS", "TXXX:LYRICS", "LYRICS", "Lyrics", "©lyr", "TEXT",
            "----:com.apple.iTunes:SYNCEDLYRICS", "----:com.apple.iTunes:LYRICS"
        )

        for (key in candidateKeys) {
            try {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) {
                    val clean = cleanExtractedLyrics(value)
                    if (clean.isNotBlank()) {
                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) return clean
                        if (fallbackUnsynced.isBlank()) fallbackUnsynced = clean
                    }
                }
            } catch (e: Exception) {}
        }

        return fallbackUnsynced
    }

    private fun extractTextFromRawField(raw: String): String {
        var s = raw.trim()
        if (s.contains("Text=\"")) {
            val start = s.indexOf("Text=\"") + 6
            val end = s.lastIndexOf("\"")
            if (end > start) {
                s = s.substring(start, end)
            }
        } else if (s.contains("Lyrics=\"")) {
            val start = s.indexOf("Lyrics=\"") + 8
            val end = s.lastIndexOf("\"")
            if (end > start) {
                s = s.substring(start, end)
            }
        }
        return s.replace("\\n", "\n").replace("\\r", "").replace("\\t", "\t")
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
                    if ((fBase == nameWithoutExt || fName == "$nameWithoutExt.lrc" || fName == "$nameWithoutExt.txt") &&
                        (fName.endsWith(".lrc") || fName.endsWith(".txt"))) {
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

    private fun getExtensionFromTrack(context: Context, track: Track): String {
        val path = track.path.lowercase()
        val fromPath = when {
            path.endsWith(".mp3") -> ".mp3"
            path.endsWith(".flac") -> ".flac"
            path.endsWith(".m4a") -> ".m4a"
            path.endsWith(".wav") -> ".wav"
            path.endsWith(".ogg") -> ".ogg"
            path.endsWith(".opus") -> ".opus"
            path.endsWith(".aac") -> ".aac"
            else -> ""
        }
        if (fromPath.isNotEmpty()) return fromPath

        // Try getting mime type from ContentResolver
        try {
            val mime = context.contentResolver.getType(track.contentUri)?.lowercase() ?: ""
            return when {
                mime.contains("flac") -> ".flac"
                mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> ".m4a"
                mime.contains("ogg") -> ".ogg"
                mime.contains("opus") -> ".opus"
                mime.contains("wav") -> ".wav"
                mime.contains("mpeg") || mime.contains("mp3") -> ".mp3"
                else -> ".mp3"
            }
        } catch (e: Exception) {}

        return ".mp3"
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
            if (parts.size == 2 && parts[0].length <= 8) {
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
