package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.Track
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
            var extractor: MediaExtractor? = null
            try {
                extractor = MediaExtractor()
                extractor.setDataSource(context, track.contentUri, null)
                if (extractor.trackCount > 0) {
                    val format = extractor.getTrackFormat(0)
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        sampleRate = "${sr / 1000.0} kHz".replace(".0 kHz", " kHz")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Extractor error: ${e.message}")
            } finally {
                try {
                    extractor?.release()
                } catch (e: Exception) {}
            }

            var artworkBytes: ByteArray? = null // Removed from Track to save RAM
            
            val lyricsResult = AudioLyricsExtractor.extractLyrics(context, track.contentUri, track.path.takeIf { it.isNotBlank() && !it.startsWith("content://") })
            var extractedLyrics = lyricsResult.lyrics ?: ""
            if (extractedLyrics.isNotBlank()) {
                extractedLyrics = cleanExtractedLyrics(extractedLyrics)
            }
            
            if (artworkBytes == null) {
                artworkBytes = retriever.embeddedPicture
            }
            
            // Intento nativo de Android como respaldo (MediaMetadataRetriever)
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

    private fun searchSidecarLyrics(audioPath: String): String {
        return try {
            val audioFile = File(audioPath)
            val parent = audioFile.parentFile ?: return ""
            val nameWithoutExt = audioFile.nameWithoutExtension

            val extensions = listOf(".lrc", ".txt", ".srt", ".vtt")
            for (ext in extensions) {
                val f = File(parent, nameWithoutExt + ext)
                if (f.exists() && f.canRead()) {
                    val text = readFileWithCharsetDetection(f)
                    if (text.isNotBlank()) {
                        return convertSubtitleToLrc(text.trim())
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


    private fun parseSyltToLrc(lyricsBytes: ByteArray): String {
        try {
            val builder = java.lang.StringBuilder()
            var offset = 0
            while (offset < lyricsBytes.size) {
                // Find end of text string (null terminated)
                var textEnd = offset
                while (textEnd < lyricsBytes.size && lyricsBytes[textEnd].toInt() != 0) {
                    textEnd++
                }
                if (textEnd >= lyricsBytes.size) break
                
                val text = String(lyricsBytes, offset, textEnd - offset, Charsets.UTF_8).trim()
                
                // Timestamp is 4 bytes integer after the null terminator
                offset = textEnd + 1
                if (offset + 3 < lyricsBytes.size) {
                    val t1 = lyricsBytes[offset].toInt() and 0xFF
                    val t2 = lyricsBytes[offset + 1].toInt() and 0xFF
                    val t3 = lyricsBytes[offset + 2].toInt() and 0xFF
                    val t4 = lyricsBytes[offset + 3].toInt() and 0xFF
                    val timestampMs = (t1 shl 24) or (t2 shl 16) or (t3 shl 8) or t4
                    
                    val minutes = timestampMs / 60000
                    val seconds = (timestampMs % 60000) / 1000
                    val hundreths = (timestampMs % 1000) / 10
                    
                    val timeStr = String.format("[%02d:%02d.%02d]", minutes, seconds, hundreths)
                    if (text.isNotBlank()) {
                        builder.append(timeStr).append(text).append("\n")
                    }
                    offset += 4
                } else {
                    break
                }
            }
            return builder.toString().trim()
        } catch (e: Exception) {
            return ""
        }
    }


    private fun convertSubtitleToLrc(content: String): String {
        if (content.contains(Regex("(\\[|<)\\d{1,3}:\\d{1,2}"))) {
            return content
        }
        
        val builder = StringBuilder()
        val lines = content.lines()
        val timeRegex = Regex("(?:\\d{2}:)?(\\d{2}):(\\d{2})[,.](\\d{2,3})\\s*-->.*")
        var currentTimestamp = ""
        
        for (line in lines) {
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1]
                val sec = match.groupValues[2]
                var milli = match.groupValues[3]
                if (milli.length == 3) milli = milli.substring(0, 2)
                currentTimestamp = "[$min:$sec.$milli]"
            } else if (line.isNotBlank() && !line.matches(Regex("^\\d+$")) && !line.startsWith("WEBVTT")) {
                if (currentTimestamp.isNotBlank()) {
                    builder.append(currentTimestamp).append(line).append("\n")
                    currentTimestamp = ""
                }
            }
        }
        
        if (builder.isNotEmpty()) {
            return builder.toString().trim()
        }
        
        return content
    }
