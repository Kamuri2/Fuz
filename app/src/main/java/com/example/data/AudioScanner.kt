package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.model.Track
import java.io.File

object AudioScanner {

    private const val TAG = "AudioScanner"

    /**
     * Scans all audio files from device MediaStore
     */
    fun scanMediaStoreAudio(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)

                val artworkUriBase = Uri.parse("content://media/external/audio/albumart")

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val sizeBytes = cursor.getLong(sizeColumn)
                    val path = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                    val year = if (yearColumn >= 0) cursor.getInt(yearColumn) else 2024
                    val trackNum = if (trackNumColumn >= 0) cursor.getInt(trackNumColumn) else 1

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val artUri = ContentUris.withAppendedId(artworkUriBase, albumId)

                    val folderName = if (path.isNotBlank()) {
                        File(path).parentFile?.name ?: "Music"
                    } else "Music"

                    val sizeMb = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))

                    val rawTrack = Track(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        contentUri = contentUri,
                        albumArtUri = artUri,
                        trackNumber = trackNum,
                        year = year,
                        fileSizeFormatted = sizeMb,
                        path = path,
                        folderName = folderName,
                        lyrics = ""
                    )

                    tracks.add(rawTrack)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore: ${e.message}")
        }

        return tracks
    }

    /**
     * Fast, lightweight scan of audio files inside a specific folder tree URI selected via Storage Access Framework (SAF)
     */
    fun scanFolderUri(context: Context, treeUri: Uri): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val folderName = rootDoc.name ?: "Custom Folder"
            scanDocumentDirectory(context, rootDoc, folderName, tracks)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder URI: ${e.message}")
        }
        return tracks
    }

    private fun scanDocumentDirectory(
        context: Context,
        dir: DocumentFile,
        folderName: String,
        outList: MutableList<Track>
    ) {
        val files = dir.listFiles()
        
        // Find LRC files in this directory
        val lrcFiles = mutableMapOf<String, DocumentFile>()
        for (file in files) {
            if (file.isFile) {
                val name = file.name ?: ""
                if (name.endsWith(".lrc", true)) {
                    val baseName = name.substringBeforeLast(".")
                    lrcFiles[baseName] = file
                }
            }
        }

        for (file in files) {
            if (file.isDirectory) {
                scanDocumentDirectory(context, file, file.name ?: folderName, outList)
            } else if (file.isFile) {
                val mime = file.type ?: ""
                val name = file.name ?: ""
                if (mime.startsWith("audio/") || name.endsWith(".mp3", true) || name.endsWith(".m4a", true) || name.endsWith(".wav", true) || name.endsWith(".flac", true) || name.endsWith(".aac", true) || name.endsWith(".ogg", true)) {
                    val id = file.uri.hashCode().toLong()
                    val title = name.substringBeforeLast(".")
                    
                    var parsedLyrics = ""
                    try {
                        lrcFiles[title]?.let { lrcFile ->
                            context.contentResolver.openInputStream(lrcFile.uri)?.bufferedReader()?.use { reader ->
                                parsedLyrics = reader.readText()
                            }
                        }
                    } catch(e: Exception) {
                        Log.e(TAG, "Error reading LRC: ${e.message}")
                    }

                    val rawTrack = Track(
                        id = id,
                        title = title,
                        artist = "Local Artist",
                        album = folderName,
                        durationMs = 0L,
                        contentUri = file.uri,
                        folderName = folderName,
                        path = name, // Use name so extension can be checked in UI
                        fileSizeFormatted = String.format("%.1f MB", file.length() / (1024.0 * 1024.0)),
                        lyrics = parsedLyrics
                    )
                    outList.add(rawTrack)
                }
            }
        }
    }
}
