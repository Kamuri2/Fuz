package com.example.data

import android.content.Context
import android.net.Uri
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.mp4.field.Mp4TagTextField
import java.io.File
import java.io.FileOutputStream

object AudioLyricsExtractor {

    data class LyricsResult(
        val lyrics: String?,
        val isSynced: Boolean,
        val source: String
    )

    fun extractLyrics(context: Context, audioUri: Uri, audioFilePath: String?): LyricsResult {
        // 1. Intentar buscar archivo externo .lrc / .txt primero
        val externalLyrics = checkAdjacentLyricsFile(audioFilePath)
        if (externalLyrics != null) {
            return LyricsResult(lyrics = externalLyrics, isSynced = true, source = "EXTERNAL_FILE")
        }

        // 2. Extraer metadatos incrustados con JAudioTagger
        var tempFile: File? = null
        try {
            val fileToRead = if (audioFilePath != null && File(audioFilePath).exists()) {
                File(audioFilePath)
            } else {
                // Si viene de ContentUri, crear archivo temporal para permitir RandomAccess
                tempFile = createTempAudioHeader(context, audioUri)
                tempFile
            }

            if (fileToRead != null && fileToRead.exists()) {
                val audioFile = AudioFileIO.read(fileToRead)
                val tag = audioFile.tag

                if (tag != null) {
                    // Letras no sincronizadas estándar
                    val unsynced = tag.getFirst(FieldKey.LYRICS)
                    if (!unsynced.isNullOrBlank()) {
                        return LyricsResult(lyrics = unsynced, isSynced = false, source = "EMBEDDED_UNSYNCED")
                    }

                    // Búsqueda específica en ID3 (USLT / SYLT / TXXX / COMM)
                    if (tag is org.jaudiotagger.tag.id3.AbstractID3v2Tag) {
                        val usltFrame = tag.getFrame("USLT") as? org.jaudiotagger.tag.id3.AbstractID3v2Frame
                        if (usltFrame != null && usltFrame.body is FrameBodyUSLT) {
                            val lyricText = (usltFrame.body as FrameBodyUSLT).lyric
                            if (!lyricText.isNullOrBlank()) {
                                return LyricsResult(lyrics = lyricText, isSynced = false, source = "ID3_USLT")
                            }
                        }
                        
                        // Buscar en SYLT (Synchronised Lyrics)
                        val syltFrame = tag.getFrame("SYLT") as? org.jaudiotagger.tag.id3.AbstractID3v2Frame
                        if (syltFrame != null && syltFrame.body is org.jaudiotagger.tag.id3.framebody.FrameBodySYLT) {
                            val syltBody = syltFrame.body as org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
                            try {
                                val lyricsBytes = syltBody.lyrics
                                if (lyricsBytes != null && lyricsBytes.isNotEmpty()) {
                                    val parsedLrc = parseSyltToLrc(lyricsBytes)
                                    if (parsedLrc.isNotBlank()) {
                                        return LyricsResult(lyrics = parsedLrc, isSynced = true, source = "ID3_SYLT")
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Buscar en TXXX:LYRICS o TXXX:UNSYNCEDLYRICS
                        val txxxFrames = tag.getFrame("TXXX")
                        if (txxxFrames != null) {
                            val framesList = if (txxxFrames is List<*>) txxxFrames else listOf(txxxFrames)
                            for (frame in framesList) {
                                val txxxFrame = frame as? org.jaudiotagger.tag.id3.AbstractID3v2Frame
                                if (txxxFrame?.body is org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX) {
                                    val body = txxxFrame.body as org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
                                    if (body.description.equals("LYRICS", ignoreCase = true) || body.description.equals("UNSYNCEDLYRICS", ignoreCase = true)) {
                                        val lyricText = body.text
                                        if (!lyricText.isNullOrBlank()) {
                                            return LyricsResult(lyrics = lyricText, isSynced = false, source = "ID3_TXXX")
                                        }
                                    }
                                }
                            }
                        }

                        // Buscar en COMM (Comments) si empiezan con Lyrics
                        val commFrames = tag.getFrame("COMM")
                        if (commFrames != null) {
                            val framesList = if (commFrames is List<*>) commFrames else listOf(commFrames)
                            for (frame in framesList) {
                                val commFrame = frame as? org.jaudiotagger.tag.id3.AbstractID3v2Frame
                                if (commFrame?.body is org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM) {
                                    val body = commFrame.body as org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM
                                    if (body.description.equals("LYRICS", ignoreCase = true) || body.text.startsWith("[") || body.text.contains("\n")) {
                                        val lyricText = body.text
                                        if (!lyricText.isNullOrBlank() && lyricText.length > 50) { // arbitrary length check for lyrics vs short comment
                                            return LyricsResult(lyrics = lyricText, isSynced = false, source = "ID3_COMM")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (tag is org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag) {
                        val synced = tag.getFirst("SYNCEDLYRICS")
                        if (!synced.isNullOrBlank()) {
                            return LyricsResult(lyrics = synced, isSynced = true, source = "VORBIS_SYNCED")
                        }
                        val unsyncedVorbis = tag.getFirst("UNSYNCEDLYRICS")
                        if (!unsyncedVorbis.isNullOrBlank()) {
                            return LyricsResult(lyrics = unsyncedVorbis, isSynced = false, source = "VORBIS_UNSYNCED")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile?.delete()
        }

        return LyricsResult(lyrics = null, isSynced = false, source = "NONE")
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

    private fun checkAdjacentLyricsFile(audioPath: String?): String? {
        if (audioPath == null) return null
        val audioFile = File(audioPath)
        if (!audioFile.exists()) return null

        val baseName = audioFile.nameWithoutExtension
        val parentDir = audioFile.parentFile ?: return null

        val extensions = listOf("lrc", "txt", "srt")
        for (ext in extensions) {
            val candidate = File(parentDir, "$baseName.$ext")
            if (candidate.exists() && candidate.canRead()) {
                return candidate.readText(Charsets.UTF_8)
            }
        }
        return null
    }

    private fun createTempAudioHeader(context: Context, uri: Uri): File? {
        return try {
            val temp = File.createTempFile("header_cache", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            temp
        } catch (e: Exception) {
            null
        }
    }
}
