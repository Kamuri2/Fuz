package com.example.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.model.Track
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

object MetadataReader {

    private const val TAG = "MetadataReader"

    /**
     * Reads complete metadata from an audio Uri using MediaMetadataRetriever
     * and deep JAudioTagger extraction for embedded LRC lyrics.
     */
    fun extractFullMetadata(context: Context, track: Track): Track {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, track.contentUri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: track.title
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: track.artist
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: track.album
            val yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.toIntOrNull() ?: track.year
            val trackNumStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackNumStr?.split("/")?.firstOrNull()?.toIntOrNull() ?: track.trackNumber
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

            // 1. Extracción profunda con JAudioTagger (Prioridad: Buscar LRC incrustado en el archivo)
            // Solo funciona si tenemos un path real (no un content:// temporal)
            if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                try {
                    Logger.getLogger("org.jaudiotagger").level = Level.OFF
                    val file = File(track.path)
                    
                    if (file.exists() && file.canRead()) {
                        val audioFile = AudioFileIO.read(file)
                        val tag = audioFile.tag
                        
                        if (tag != null) {
                            // Buscar en múltiples frames donde los metadatos incrustan LRC
                            
                            // A) Intentar encontrar letras sincronizadas (LRC) en campos personalizados
                            var tagLyrics = tag.getFirst("SYNCEDLYRICS") // Común en FLAC (Vorbis)
                            
                            if (tagLyrics.isNullOrBlank()) {
                                tagLyrics = tag.getFirst("TXXX:SYNCEDLYRICS") // Común en MP3 (ID3v2 Custom Frame)
                            }
                            if (tagLyrics.isNullOrBlank()) {
                                tagLyrics = tag.getFirst("SYLT") // Frame oficial ID3v2 para letras sincronizadas
                            }
                            
                            // B) Si no hay LRC sincronizado, caer al frame estándar de letras (USLT)
                            if (tagLyrics.isNullOrBlank()) {
                                tagLyrics = tag.getFirst(FieldKey.LYRICS)
                            }

                            if (!tagLyrics.isNullOrBlank()) {
                                extractedLyrics = cleanExtractedLyrics(tagLyrics)
                            }
                            
                            // Extraer carátula si el retriever nativo falló
                            if (artworkBytes == null) {
                                val artwork = tag.firstArtwork
                                if (artwork != null && artwork.binaryData != null) {
                                    artworkBytes = artwork.binaryData
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "JAudioTagger error: ${e.message}")
                }
            }

            // 2. Intento nativo de Android como respaldo (MediaMetadataRetriever)
            if (extractedLyrics.isBlank()) {
                try {
                    val rawLyrics = retriever.extractMetadata(1000) // Código 1000 = METADATA_KEY_LYRICS
                    if (!rawLyrics.isNullOrBlank()) {
                        extractedLyrics = cleanExtractedLyrics(rawLyrics)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No embedded lyrics found via MediaMetadataRetriever")
                }
            }

            // 3. Buscar archivos sueltos (.lrc / .txt) solo si el archivo no tenía nada incrustado
            if (extractedLyrics.isBlank() && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                try {
                    val audioFile = File(track.path)
                    if (audioFile.exists()) {
                        val parent = audioFile.parentFile
                        val nameWithoutExt = audioFile.nameWithoutExtension
                        
                        val lrcFile = File(parent, "$nameWithoutExt.lrc")
                        val txtFile = File(parent, "$nameWithoutExt.txt")
                        
                        if (lrcFile.exists() && lrcFile.canRead()) {
                            extractedLyrics = lrcFile.readText(Charsets.UTF_8).trim()
                        } else if (txtFile.exists() && txtFile.canRead()) {
                            extractedLyrics = txtFile.readText(Charsets.UTF_8).trim()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error reading sidecar lyrics file: ${e.message}")
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
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    /**
     * Limpia la metadata de letras incrustadas.
     * JAudioTagger a veces devuelve el frame con cabeceras de idioma u otros datos (ej. "eng||[00:12.00]Letra").
     */
    private fun cleanExtractedLyrics(raw: String): String {
        var cleaned = raw.trim()
        
        // Si el tag incrustado viene con separador de JAudioTagger
        if (cleaned.contains("||")) {
            val parts = cleaned.split("||", limit = 2)
            if (parts.size == 2 && parts[0].length <= 4) {
                cleaned = parts[1]
            }
        }
        
        // Limpiar prefijos comunes de idioma que algunos editores incrustan
        val commonPrefixes = listOf("eng|", "spa|", "jpn|", "XXX|", "xxx|")
        for (prefix in commonPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length)
            }
        }
        
        return cleaned.trim()
    }
}