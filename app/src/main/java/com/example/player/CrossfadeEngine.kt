package com.example.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Two-player Crossfade Engine for seamless, gapless track transitions.
 *
 * Implements an S-curve (smoothstep) volume fade between the outgoing active player
 * and incoming next player over a crossfade window (default 0.5 seconds), eliminating
 * audio cuts and abrupt transitions between consecutive songs.
 */
class CrossfadeEngine(
    private val context: Context,
    var crossfadeDurationMs: Long = 100L,
    private val playerFactory: () -> ExoPlayer,
    private val onHandover: (newPlayer: ExoPlayer) -> Unit
) {
    private var activePlayer: ExoPlayer? = null
    private var incomingPlayer: ExoPlayer? = null
    var isCrossfading = false
        private set
    private var crossfadeStartTime = 0L
    private var userVolume = 1f
    private var preloadedMediaItem: MediaItem? = null

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (!isCrossfading) return
            val elapsed = SystemClock.elapsedRealtime() - crossfadeStartTime
            val duration = crossfadeDurationMs.coerceAtLeast(100L)
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

            // Curva S (smoothstep: t * t * (3 - 2t)) para un fade más natural
            val smooth = progress * progress * (3f - 2f * progress)

            activePlayer?.volume = (1f - smooth) * userVolume
            incomingPlayer?.volume = smooth * userVolume

            if (progress >= 1f) {
                // Handover: Incoming player se convierte en el reproductor principal
                val newActive = incomingPlayer
                activePlayer?.release()
                activePlayer = newActive
                incomingPlayer = null
                isCrossfading = false
                preloadedMediaItem = null
                if (newActive != null) {
                    newActive.volume = userVolume
                    onHandover(newActive)
                }
            } else {
                handler.postDelayed(this, 15) // tick cada 15ms para suavidad en 100ms
            }
        }
    }

    fun attachActivePlayer(player: ExoPlayer, volume: Float = 1f) {
        activePlayer = player
        userVolume = volume
    }

    fun setVolume(vol: Float) {
        userVolume = vol
        if (!isCrossfading) {
            activePlayer?.volume = vol
        }
    }

    /** Pre-carga la siguiente pista con anticipación (ej. 10s antes del fin) */
    fun preloadNext(nextItem: MediaItem) {
        if (isCrossfading) return
        if (incomingPlayer == null || preloadedMediaItem?.mediaId != nextItem.mediaId) {
            incomingPlayer?.release()
            preloadedMediaItem = nextItem
            incomingPlayer = playerFactory().apply {
                setMediaItem(nextItem)
                prepare()
                playWhenReady = false // Solo pre-carga, no suena aún
                volume = 0f
            }
        }
    }

    /** Inicia el crossfade en los últimos N ms */
    fun startCrossfade(nextItem: MediaItem, customDurationMs: Long = crossfadeDurationMs) {
        if (isCrossfading || activePlayer == null) return
        crossfadeDurationMs = customDurationMs.coerceAtLeast(100L)

        if (incomingPlayer == null || preloadedMediaItem?.mediaId != nextItem.mediaId) {
            incomingPlayer?.release()
            incomingPlayer = playerFactory().apply {
                setMediaItem(nextItem)
                prepare()
            }
        }

        val incoming = incomingPlayer ?: return
        incoming.volume = 0f
        incoming.playWhenReady = true
        incoming.play()

        isCrossfading = true
        crossfadeStartTime = SystemClock.elapsedRealtime()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    fun cancelCrossfade() {
        if (isCrossfading || incomingPlayer != null) {
            handler.removeCallbacks(ticker)
            isCrossfading = false
            incomingPlayer?.release()
            incomingPlayer = null
            preloadedMediaItem = null
            activePlayer?.volume = userVolume
        }
    }

    fun release() {
        handler.removeCallbacks(ticker)
        isCrossfading = false
        incomingPlayer?.release()
        incomingPlayer = null
        preloadedMediaItem = null
        activePlayer = null
    }
}
