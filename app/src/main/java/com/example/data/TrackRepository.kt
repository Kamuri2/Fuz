package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.TrackEntity
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TrackRepository {
    private const val TAG = "TrackRepository"
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                // Forcing a DB clear to remove any corrupted lyrics cache from previous builds
                db.trackDao().clear()
                val cached = db.trackDao().getAllTracks().map { it.toTrack() }
                if (cached.isNotEmpty()) {
                    _tracks.value = cached
                    Log.d(TAG, "Loaded ${cached.size} cached tracks from persistent Room database on app start")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TrackRepository: ${e.message}")
            }
        }
    }

    suspend fun saveScannedTracks(context: Context, newTracks: List<Track>) {
        if (newTracks.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val existing = db.trackDao().getAllTracks().associateBy { it.id }
                
                val entitiesToSave = newTracks.map { track ->
                    val cached = existing[track.id]
                    val mergedLyrics = if (track.lyrics.isNotBlank()) track.lyrics else (cached?.lyrics ?: "")
                    val mergedTrack = track.copy(lyrics = mergedLyrics)
                    TrackEntity.fromTrack(mergedTrack)
                }
                
                db.trackDao().insertOrUpdateTracks(entitiesToSave)
                _tracks.value = entitiesToSave.map { it.toTrack() }
                Log.d(TAG, "Successfully cached ${entitiesToSave.size} tracks to Room DB")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving scanned tracks to Room: ${e.message}")
            }
        }
    }

    suspend fun updateTrackLyrics(context: Context, trackId: Long, lyrics: String) {
        if (lyrics.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.trackDao().updateLyrics(trackId, lyrics)
                
                val updated = _tracks.value.map {
                    if (it.id == trackId) it.copy(lyrics = lyrics) else it
                }
                _tracks.value = updated
                Log.d(TAG, "Updated persistent lyrics for track ID: $trackId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating track lyrics: ${e.message}")
            }
        }
    }
}
