package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.R

class PlaybackService : Service() {

    private lateinit var mediaSession: MediaSession
    private lateinit var playerManager: AudioPlayerManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val CHANNEL_ID = "fuzion_playback_channel"

    override fun onCreate() {
        super.onCreate()
        playerManager = AudioPlayerManager.getInstance(applicationContext)

        mediaSession = MediaSession(this, "FuzionPlaybackService").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { playerManager.togglePlayPause() }
                override fun onPause() { playerManager.togglePlayPause() }
                override fun onSkipToNext() { playerManager.nextTrack() }
                override fun onSkipToPrevious() { playerManager.previousTrack() }
                override fun onSeekTo(pos: Long) { playerManager.seekTo(pos) }
            })
            isActive = true
        }

        createNotificationChannel()

        scope.launch {
            playerManager.currentTrack.collect { track ->
                updateNotificationAndSession()
            }
        }
        scope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                updateNotificationAndSession()
            }
        }
    }

    private fun updateNotificationAndSession() {
        val track = playerManager.currentTrack.value ?: return
        val isPlaying = playerManager.isPlaying.value

        // Update MediaSession
        val stateBuilder = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO)
            .setState(if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, playerManager.currentPositionMs.value, 1.0f)
        mediaSession.setPlaybackState(stateBuilder.build())

        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)

        var albumArtBitmap: android.graphics.Bitmap? = null
        try {
            val mmr = android.media.MediaMetadataRetriever()
            if (track.path.isNotBlank() && !track.path.startsWith("content://")) {
                try {
                    mmr.setDataSource(track.path)
                } catch (e:Exception) {
                    mmr.setDataSource(this, track.contentUri)
                }
            } else {
                mmr.setDataSource(this, track.contentUri)
            }
            val rawBytes = mmr.embeddedPicture
            mmr.release()
            if (rawBytes != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(rawBytes))
                    albumArtBitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.setTargetSampleSize(2)
                    }
                } else {
                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                    albumArtBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                }
            }
        } catch (e: Exception) {}
        
        if (albumArtBitmap == null && track.albumArtUri != null) {
            try {
                // For MediaStore URIs, we should also try to limit size if possible, but keeping it simple for now
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, track.albumArtUri)
                    albumArtBitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        var sampleSize = 1
                        if (info.size.height > 300 || info.size.width > 300) {
                            var halfHeight = info.size.height / 2
                            var halfWidth = info.size.width / 2
                            while (halfHeight / sampleSize >= 300 && halfWidth / sampleSize >= 300) {
                                sampleSize *= 2
                            }
                        }
                        decoder.setTargetSampleSize(sampleSize)
                        // decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val pfd = contentResolver.openFileDescriptor(track.albumArtUri, "r")
                    if (pfd != null) {
                        val fd = pfd.fileDescriptor
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        android.graphics.BitmapFactory.decodeFileDescriptor(fd, null, options)
                        var inSampleSize = 1
                        if (options.outHeight > 300 || options.outWidth > 300) {
                            val halfHeight = options.outHeight / 2
                            val halfWidth = options.outWidth / 2
                            while (halfHeight / inSampleSize >= 300 && halfWidth / inSampleSize >= 300) {
                                inSampleSize *= 2
                            }
                        }
                        val finalOptions = android.graphics.BitmapFactory.Options().apply {
                            this.inSampleSize = inSampleSize
                        }
                        albumArtBitmap = android.graphics.BitmapFactory.decodeFileDescriptor(fd, null, finalOptions)
                        pfd.close()
                    }
                }
            } catch (e: Exception) {}
        }
        
        if (albumArtBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtBitmap)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        // Build Notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }

        builder.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album)
            .setContentIntent(pendingIntent)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            .setStyle(android.app.Notification.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.sessionToken))

        // Actions
        val prevIntent = Intent(this, PlaybackService::class.java).setAction("PREV")
        builder.addAction(android.app.Notification.Action.Builder(android.R.drawable.ic_media_previous, "Previous", PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE)).build())

        if (isPlaying) {
            val pauseIntent = Intent(this, PlaybackService::class.java).setAction("PAUSE")
            builder.addAction(android.app.Notification.Action.Builder(android.R.drawable.ic_media_pause, "Pause", PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)).build())
        } else {
            val playIntent = Intent(this, PlaybackService::class.java).setAction("PLAY")
            builder.addAction(android.app.Notification.Action.Builder(android.R.drawable.ic_media_play, "Play", PendingIntent.getService(this, 3, playIntent, PendingIntent.FLAG_IMMUTABLE)).build())
        }

        val nextIntent = Intent(this, PlaybackService::class.java).setAction("NEXT")
        builder.addAction(android.app.Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_IMMUTABLE)).build())

        if (albumArtBitmap != null) {
            builder.setLargeIcon(albumArtBitmap)
        }

        if (isPlaying) {
            startForeground(1, builder.build())
        } else {
            val notification = builder.build()
            startForeground(1, notification)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                stopForeground(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY", "PAUSE" -> playerManager.togglePlayPause()
            "NEXT" -> playerManager.nextTrack()
            "PREV" -> playerManager.previousTrack()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        playerManager.release()
    }
}