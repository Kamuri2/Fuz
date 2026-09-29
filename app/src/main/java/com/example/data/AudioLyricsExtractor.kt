package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object AudioLyricsExtractor {
    private const val TAG = "AudioLyricsExtractor"

    data class LyricsResult(
        val lyrics: String?,
        val isSynced: Boolean,
        val source: String
    )

    fun extractLyrics(context: Context, audioUri: Uri, audioFilePath: String?): LyricsResult {
        // 1. PRIMARY SOURCE: Embedded lyrics inside audio file tags via JAudioTagger
        // MediaMetadataRetriever does not expose USLT/UNSYNCEDLYRICS/Vorbis tags, so JAudioTagger is essential
        val embeddedResult = extractEmbeddedLyrics(context, audioUri, audioFilePath)
        if (embeddedResult != null && !embeddedResult.lyrics.isNullOrBlank()) {
            return embeddedResult
        }

        // 2. SECONDARY SOURCE: Sidecar file (.lrc, .txt, .srt) adjacent to the audio track
        val externalLyrics = checkAdjacentLyricsFile(audioFilePath)
        if (externalLyrics != null && externalLyrics.isNotBlank()) {
            val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(externalLyrics)
            return LyricsResult(lyrics = externalLyrics.trim(), isSynced = isSynced, source = "EXTERNAL_FILE")
        }

        // 3. TERTIARY SOURCE: Direct deep scan for Opus/Ogg Vorbis packets
        if (audioFilePath != null) {
            val deepScanResult = getOpusLyricsDeepScan(audioFilePath)
            if (deepScanResult != null && !deepScanResult.lyrics.isNullOrBlank()) {
                return deepScanResult
            }
        }

        return LyricsResult(lyrics = null, isSynced = false, source = "NONE")
    }

    private fun extractEmbeddedLyrics(context: Context, audioUri: Uri, audioFilePath: String?): LyricsResult? {
        var tempFile: File? = null
        try {
            val fileToRead = if (audioFilePath != null && File(audioFilePath).exists() && File(audioFilePath).canRead()) {
                File(audioFilePath)
            } else if (audioUri != Uri.EMPTY) {
                tempFile = createTempAudioHeader(context, audioUri)
                tempFile
            } else {
                null
            }

            if (fileToRead != null && fileToRead.exists()) {
                val audioFile = AudioFileIO.read(fileToRead)
                val tag: Tag? = audioFile.tag

                if (tag != null) {
                    // Check synchronized ID3v2 SYLT frame first (highest fidelity sync lyrics)
                    if (tag is AbstractID3v2Tag) {
                        try {
                            val syltFrame = tag.getFrame("SYLT") as? AbstractID3v2Frame
                            if (syltFrame != null && syltFrame.body is FrameBodySYLT) {
                                val syltBody = syltFrame.body as FrameBodySYLT
                                val lyricsBytes = syltBody.lyrics
                                if (lyricsBytes != null && lyricsBytes.isNotEmpty()) {
                                    val parsedLrc = parseSyltToLrc(lyricsBytes)
                                    if (parsedLrc.isNotBlank()) {
                                        return LyricsResult(lyrics = parsedLrc, isSynced = true, source = "JAUDIOTAGGER_ID3_SYLT")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "SYLT frame parsing failed: ${e.message}")
                        }

                        // Check ID3v2 USLT frame (Unsynchronized lyric/text transcription)
                        try {
                            val usltFrame = tag.getFrame("USLT") as? AbstractID3v2Frame
                            if (usltFrame != null && usltFrame.body is FrameBodyUSLT) {
                                val lyricText = (usltFrame.body as FrameBodyUSLT).lyric
                                if (!lyricText.isNullOrBlank()) {
                                    val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(lyricText)
                                    return LyricsResult(lyrics = lyricText.trim(), isSynced = isSynced, source = "JAUDIOTAGGER_ID3_USLT")
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "USLT frame check failed: ${e.message}")
                        }
                    }

                    // FieldKey.LYRICS is jaudiotagger's standard mapping for MP3 (USLT) and FLAC/OGG (LYRICS)
                    try {
                        val standardLyrics = tag.getFirst(FieldKey.LYRICS)
                        if (!standardLyrics.isNullOrBlank()) {
                            val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(standardLyrics)
                            return LyricsResult(lyrics = standardLyrics.trim(), isSynced = isSynced, source = "JAUDIOTAGGER_FIELD_LYRICS")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "tag.getFirst(FieldKey.LYRICS) failed: ${e.message}")
                    }

                    // For FLAC & Vorbis Comment tags: check specific tags: LYRICS, UNSYNCEDLYRICS, UNSYNCED_LYRICS
                    if (tag is FlacTag || tag is VorbisCommentTag) {
                        val candidateKeys = listOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED_LYRICS", "SYNCEDLYRICS", "TEXT")
                        for (key in candidateKeys) {
                            try {
                                val valText = tag.getFirst(key)
                                if (!valText.isNullOrBlank()) {
                                    val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(valText)
                                    return LyricsResult(lyrics = valText.trim(), isSynced = isSynced, source = "JAUDIOTAGGER_VORBIS_$key")
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    // Check ID3v2 TXXX user-defined text frames (often used for UNSYNCEDLYRICS)
                    if (tag is AbstractID3v2Tag) {
                        try {
                            val txxxFrames = tag.getFrame("TXXX")
                            if (txxxFrames != null) {
                                val framesList = if (txxxFrames is List<*>) txxxFrames else listOf(txxxFrames)
                                for (frame in framesList) {
                                    val txxxFrame = frame as? AbstractID3v2Frame
                                    if (txxxFrame?.body is FrameBodyTXXX) {
                                        val body = txxxFrame.body as FrameBodyTXXX
                                        if (body.description.equals("LYRICS", ignoreCase = true) ||
                                            body.description.equals("UNSYNCEDLYRICS", ignoreCase = true) ||
                                            body.description.equals("UNSYNCED_LYRICS", ignoreCase = true)
                                        ) {
                                            val lyricText = body.text
                                            if (!lyricText.isNullOrBlank()) {
                                                val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(lyricText)
                                                return LyricsResult(lyrics = lyricText.trim(), isSynced = isSynced, source = "JAUDIOTAGGER_ID3_TXXX")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "TXXX check failed: ${e.message}")
                        }
                    }

                    // General fields iterator fallback
                    try {
                        val fields = tag.fields
                        while (fields.hasNext()) {
                            val field = fields.next()
                            val lowerId = field.id.lowercase()
                            if (lowerId.contains("lyric") || lowerId.contains("sylt") || lowerId.contains("uslt")) {
                                if (!field.isBinary) {
                                    val content = field.toString()
                                    if (!content.isNullOrBlank() && content.length > 15) {
                                        val cleanContent = tag.getFirst(field.id).takeIf { it.isNotBlank() } ?: content
                                        val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(cleanContent)
                                        return LyricsResult(lyrics = cleanContent.trim(), isSynced = isSynced, source = "JAUDIOTAGGER_FIELD_${field.id.uppercase()}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "General fields iterator failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "extractEmbeddedLyrics error: ${e.message}")
        } finally {
            tempFile?.delete()
        }
        return null
    }

    private fun parseSyltToLrc(lyricsBytes: ByteArray): String {
        try {
            val builder = java.lang.StringBuilder()
            var offset = 0
            while (offset < lyricsBytes.size) {
                var textEnd = offset
                while (textEnd < lyricsBytes.size && lyricsBytes[textEnd].toInt() != 0) {
                    textEnd++
                }
                if (textEnd >= lyricsBytes.size) break
                
                val text = String(lyricsBytes, offset, textEnd - offset, Charsets.UTF_8).trim()
                
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
    
    private fun getOpusLyricsDeepScan(filePath: String): LyricsResult? {
        try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val inputStream = file.inputStream()
            val buffer = ByteArray(5 * 1024 * 1024) 
            val bytesRead = inputStream.read(buffer)
            inputStream.close()
            
            if (bytesRead < 27) return null
            
            val packetData = ByteArrayOutputStream()
            var offset = 0
            var capturingTags = false
            
            while (offset < bytesRead - 27) {
                if (buffer[offset] == 'O'.code.toByte() && buffer[offset+1] == 'g'.code.toByte() && 
                    buffer[offset+2] == 'g'.code.toByte() && buffer[offset+3] == 'S'.code.toByte()) {
                    
                    val segments = buffer[offset + 26].toInt() and 0xFF
                    val segmentTableOffset = offset + 27
                    var pageDataOffset = segmentTableOffset + segments
                    
                    if (pageDataOffset > bytesRead) break
                    
                    for (i in 0 until segments) {
                        if (segmentTableOffset + i >= bytesRead) break
                        val segmentLength = buffer[segmentTableOffset + i].toInt() and 0xFF
                        if (pageDataOffset + segmentLength > bytesRead) break
                        
                        if (segmentLength >= 8 && 
                            buffer[pageDataOffset] == 'O'.code.toByte() && 
                            buffer[pageDataOffset+1] == 'p'.code.toByte() &&
                            buffer[pageDataOffset+2] == 'u'.code.toByte() &&
                            buffer[pageDataOffset+3] == 's'.code.toByte() &&
                            buffer[pageDataOffset+4] == 'T'.code.toByte() &&
                            buffer[pageDataOffset+5] == 'a'.code.toByte() &&
                            buffer[pageDataOffset+6] == 'g'.code.toByte() &&
                            buffer[pageDataOffset+7] == 's'.code.toByte()) {
                            capturingTags = true
                        }
                        
                        if (capturingTags) {
                            packetData.write(buffer, pageDataOffset, segmentLength)
                            if (segmentLength < 255) {
                                offset = bytesRead 
                                break
                            }
                        }
                        pageDataOffset += segmentLength
                    }
                    if (offset == bytesRead) break
                    offset = pageDataOffset
                } else {
                    offset++
                }
            }
            
            if (packetData.size() > 8) {
                val packetBytes = packetData.toByteArray()
                var pOffset = 8 // Skip "OpusTags"
                
                fun readInt32LE(bytes: ByteArray, idx: Int): Int {
                    if (idx + 3 >= bytes.size) return 0
                    return (bytes[idx].toInt() and 0xFF) or 
                           ((bytes[idx+1].toInt() and 0xFF) shl 8) or 
                           ((bytes[idx+2].toInt() and 0xFF) shl 16) or 
                           ((bytes[idx+3].toInt() and 0xFF) shl 24)
                }
                
                val vendorLen = readInt32LE(packetBytes, pOffset)
                pOffset += 4 + vendorLen
                if (pOffset < packetBytes.size) {
                    val commentListLen = readInt32LE(packetBytes, pOffset)
                    pOffset += 4
                    
                    for (i in 0 until commentListLen) {
                        if (pOffset + 4 > packetBytes.size) break
                        val commentLen = readInt32LE(packetBytes, pOffset)
                        pOffset += 4
                        
                        if (pOffset + commentLen > packetBytes.size) break
                        val commentStr = String(packetBytes, pOffset, commentLen, Charsets.UTF_8)
                        pOffset += commentLen
                        
                        val lower = commentStr.lowercase()
                        if (lower.startsWith("lyrics=") || lower.startsWith("sylt=") || lower.startsWith("uslt=") || lower.startsWith("text=")) {
                            val content = commentStr.substring(commentStr.indexOf('=') + 1).trim()
                            val isSynced = Regex("\\[\\d{2}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(content)
                            return LyricsResult(lyrics = content, isSynced = isSynced, source = "OPUS_TAGS_PARSER")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
