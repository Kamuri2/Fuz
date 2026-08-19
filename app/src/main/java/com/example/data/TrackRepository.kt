package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.TrackEntity
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    /**
     * Scans and enriches all tracks in a single upfront parallel pass, saving all metadata,
     * tags and lyrics permanently to the local Room database so it never needs to be re-scanned.
     */
    suspend fun saveScannedTracks(context: Context, newTracks: List<Track>, forceRescan: Boolean = false): List<Track> {
        if (newTracks.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val existing = db.trackDao().getAllTracks().associateBy { it.id }

                // 1. Separate tracks into already fully-cached vs tracks that need tag/lyrics extraction
                val fullyEnrichedTracks = mutableListOf<Track>()
                val tracksNeedingEnrichment = mutableListOf<Track>()

                for (track in newTracks) {
                    val cached = existing[track.id]
                    // If cached track already exists and has complete metadata or lyrics, reuse it directly
                    if (!forceRescan && cached != null && (cached.lyrics.isNotBlank() || (cached.genre.isNotBlank() && cached.bitrate.isNotBlank()))) {
                        fullyEnrichedTracks.add(cached.toTrack())
                    } else {
                        tracksNeedingEnrichment.add(track)
                    }
                }

                // 2. Process tracks needing enrichment in parallel batches (16 concurrent extractions)
                val newlyEnriched = if (tracksNeedingEnrichment.isNotEmpty()) {
                    Log.d(TAG, "Extracting full metadata and lyrics upfront for ${tracksNeedingEnrichment.size} tracks...")
                    tracksNeedingEnrichment.chunked(16).flatMap { chunk ->
                        chunk.map { track ->
                            async(Dispatchers.IO) {
                                try {
                                    val cached = existing[track.id]
                                    var enriched = MetadataReader.extractFullMetadata(context, track)
                                    // If cached had lyrics, preserve it if extractor didn't find new ones
                                    if (enriched.lyrics.isBlank() && cached != null && cached.lyrics.isNotBlank()) {
                                        enriched = enriched.copy(lyrics = cached.lyrics)
                                    }
                                    enriched
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error enriching track ${track.title}: ${e.message}")
                                    track
                                }
                            }
                        }.awaitAll()
                    }
                } else {
                    emptyList()
                }

                // 3. Combine all tracks and save to Room
                val allEnrichedTracks = (fullyEnrichedTracks + newlyEnriched).distinctBy { it.id }.sortedBy { it.title.lowercase() }
                val entitiesToSave = allEnrichedTracks.map { TrackEntity.fromTrack(it) }

                db.trackDao().insertOrUpdateTracks(entitiesToSave)

                val validIds = allEnrichedTracks.map { it.id }
                if (validIds.isNotEmpty()) {
                    try {
                        db.trackDao().deleteMissing(validIds)
                    } catch (e: Exception) {}
                }

                _tracks.value = allEnrichedTracks
                Log.d(TAG, "Successfully cached ${allEnrichedTracks.size} fully enriched tracks in Room DB")
                allEnrichedTracks
            } catch (e: Exception) {
                Log.e(TAG, "Error saving scanned tracks to Room: ${e.message}")
                newTracks
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
