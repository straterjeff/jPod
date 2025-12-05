package com.example.android_music_player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.android_music_player.MainActivity
import com.example.android_music_player.R

/**
 * Service for managing music playback in the background
 */
class MusicService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var wrappedPlayer: Player
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeMediaSession()
        createNotificationChannel()
    }
    
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Wrap player to intercept stop() calls and convert to pause() when playing
        // This ensures earbud button presses pause instead of stop
        wrappedPlayer = object : ForwardingPlayer(player) {
            private var shouldInterceptStop = false
            
            override fun stop() {
                // If player is playing, pause instead of stop (for earbud button presses)
                if (player.isPlaying) {
                    shouldInterceptStop = true
                    player.pause()
                } else {
                    // Only stop if already paused/stopped (explicit stop from UI)
                    player.stop()
                }
            }
            
            override fun pause() {
                shouldInterceptStop = false
                super.pause()
            }
            
            override fun play() {
                shouldInterceptStop = false
                super.play()
            }
        }
    }
    
    private fun initializeMediaSession() {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Use wrappedPlayer which intercepts stop() calls and converts them to pause() when playing
        // This ensures earbud button presses pause instead of stop
        mediaSession = MediaSession.Builder(this, wrappedPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    /**
     * Get the ExoPlayer instance
     */
    fun getPlayer(): ExoPlayer = player
    
    /**
     * Play a list of songs starting from a specific index
     */
    fun playSongs(songs: List<com.example.android_music_player.data.Song>, startIndex: Int = 0) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()
    }
    
    /**
     * Play a single song
     */
    fun playSong(song: com.example.android_music_player.data.Song) {
        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        player.stop()
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleModeEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }
}
