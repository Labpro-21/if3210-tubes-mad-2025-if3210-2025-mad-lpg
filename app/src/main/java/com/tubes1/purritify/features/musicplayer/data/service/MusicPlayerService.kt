package com.tubes1.purritify.features.musicplayer.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.tubes1.purritify.MainActivity
import com.tubes1.purritify.R
import com.tubes1.purritify.core.domain.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException


class MusicPlayerService : Service() {
    private val binder = MusicPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = -1
    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "purritify_music_channel"
        const val NOTIFICATION_ID = 1337
        const val ACTION_PLAY = "com.tubes1.purritify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.tubes1.purritify.ACTION_PAUSE"
        const val ACTION_NEXT = "com.tubes1.purritify.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.tubes1.purritify.ACTION_PREVIOUS"
        const val ACTION_STOP_FOREGROUND = "com.tubes1.purritify.ACTION_STOP_FOREGROUND"

        // For Glide image loading dimensions
        private const val NOTIFICATION_ART_SIZE = 256 // px
        private const val METADATA_ART_SIZE = 512 // px
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "PurritifyMediaSession").apply {
            setCallback(mediaSessionCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }

        initializeMediaPlayer()
        observePlayerStateForNotification()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener { playNext() }
            setOnPreparedListener { mp ->
                _duration.value = mp.duration.toLong()
                mp.start()
                _isPlaying.value = true
                startPositionUpdates()
                updateMediaSessionStateAndNotification()
                mediaSession.isActive = true
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerService", "MediaPlayer Error: What: $what, Extra: $extra")
                stopPositionUpdates()
                _isPlaying.value = false
                // Attempt to reset or re-prepare if appropriate, or notify UI of error
                // For now, just update state
                updateMediaSessionStateAndNotification()
                true // Indicates the error was handled
            }
        }
    }

    private fun observePlayerStateForNotification() {
        serviceScope.launch {
            isPlaying.collect {
                updateMediaSessionStateAndNotification()
            }
        }
        serviceScope.launch {
            currentSong.collect {
                updateMediaSessionStateAndNotification()
            }
        }
        // Optionally observe _duration and _currentPosition if they should also trigger full notification updates
        // For now, _currentPosition updates PlaybackStateCompat, which is good enough for seek bar.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicPlayerService", "onStartCommand received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY -> playPause()
            ACTION_PAUSE -> playPause() // MediaSessionCompat often sends PLAY for toggle
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP_FOREGROUND -> {
                stopPlaybackAndNotification()
                stopSelf() // Important to stop the service if it's meant to be fully stopped
            }
            else -> {
                // Let MediaButtonReceiver handle other media button events
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }
        return START_STICKY // Or START_NOT_STICKY if you don't want it to auto-restart
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound, can be DEFAULT if heads-up is desired
            ).apply {
                description = "Controls for music playback"
                setSound(null, null) // Ensure no sound for LOW importance
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Log.d("MediaSessionCallback", "onPlay")
            playPause()
        }

        override fun onPause() {
            Log.d("MediaSessionCallback", "onPause")
            playPause()
        }

        override fun onSkipToNext() {
            Log.d("MediaSessionCallback", "onSkipToNext")
            playNext()
        }

        override fun onSkipToPrevious() {
            Log.d("MediaSessionCallback", "onSkipToPrevious")
            playPrevious()
        }

        override fun onStop() {
            Log.d("MediaSessionCallback", "onStop")
            stopPlaybackAndNotification() // This stops playback and removes notification
        }

        override fun onSeekTo(pos: Long) {
            Log.d("MediaSessionCallback", "onSeekTo: $pos")
            seekTo(pos)
            // PlaybackState is updated by startPositionUpdates or immediately by seekTo if needed
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        _currentSong.value = song
        currentQueue = queue
        currentSongIndex = queue.indexOf(song).takeIf { it >= 0 } ?: 0

        initializeMediaPlayer() // Creates a new MediaPlayer instance

        mediaPlayer?.apply {
            try {
                // MediaPlayer can handle http/https URLs directly
                setDataSource(applicationContext, Uri.parse(song.path))
                prepareAsync() // Triggers onPreparedListener when ready
                // _isPlaying will be set to true in onPrepared
            } catch (e: IOException) {
                Log.e("MusicPlayerService", "Error setting data source for ${song.path}", e)
                _isPlaying.value = false
                _currentSong.value = null // Or handle error state appropriately
                updateMediaSessionStateAndNotification()
            } catch (e: IllegalStateException) {
                Log.e("MusicPlayerService", "IllegalState setting data source for ${song.path}", e)
                _isPlaying.value = false
                _currentSong.value = null
                updateMediaSessionStateAndNotification()
            }
        }
    }

    fun playPause() {
        mediaPlayer?.let {
            if (_isPlaying.value) { // Use StateFlow value as source of truth
                it.pause()
                _isPlaying.value = false
                stopPositionUpdates()
            } else {
                // Ensure player is prepared or can be started.
                // If player was simply paused, it.start() is fine.
                // If player is in IDLE or INITIALIZED state (e.g., after error or new song),
                // it needs prepareAsync() again. playSong() handles this.
                try {
                    it.start()
                    _isPlaying.value = true
                    startPositionUpdates()
                } catch (e: IllegalStateException) {
                    Log.e("MusicPlayerService", "MediaPlayer not ready to start, attempting to replay current song.", e)
                    // This might happen if player is not prepared. Try to play the current song again.
                    currentSong.value?.let { current -> playSong(current, currentQueue) }
                        ?: Log.e("MusicPlayerService", "No current song to replay.")
                }
            }
            updateMediaSessionStateAndNotification()
        } ?: run {
            // No media player, try to play if there's a song in queue
            if (currentQueue.isNotEmpty() && currentSongIndex != -1) {
                playSong(currentQueue[currentSongIndex], currentQueue)
            } else {
                Log.w("MusicPlayerService", "Play/Pause called but no media player and no song.")
            }
        }
    }

    fun playNext() {
        if (currentQueue.isEmpty()) return
        currentSongIndex = (currentSongIndex + 1) % currentQueue.size
        playSong(currentQueue[currentSongIndex], currentQueue)
    }

    fun playPrevious() {
        if (currentQueue.isEmpty()) return

        // If song played for more than 3 seconds, restart it. Otherwise, play previous.
        if ((mediaPlayer?.currentPosition ?: 0) > 3000 && _isPlaying.value) {
            seekTo(0)
            if (!_isPlaying.value) { // If it was paused and we seek to 0, start playing
                mediaPlayer?.start()
                _isPlaying.value = true
                startPositionUpdates()
                updateMediaSessionStateAndNotification()
            }
            return
        }

        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else currentQueue.size - 1
        if (currentSongIndex < 0 && currentQueue.isNotEmpty()) currentSongIndex = 0 // Ensure valid index

        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex], currentQueue)
        }
    }


    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
        // Update playback state immediately after seek
        updateMediaSessionPlaybackState(_currentSong.value, position, _isPlaying.value)
    }

    private fun startPositionUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) { // Use isActive to ensure coroutine stops when scope is cancelled
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        val currentPos = mediaPlayer!!.currentPosition.toLong()
                        _currentPosition.value = currentPos
                        // Update PlaybackState frequently for smooth seekbar progression
                        updateMediaSessionPlaybackState(_currentSong.value, currentPos, _isPlaying.value)
                    } catch (e: IllegalStateException) {
                        Log.w("MusicPlayerService", "Error getting current position: ${e.message}")
                        // MediaPlayer might have been released or in an error state
                        stopPositionUpdates() // Stop updates if player is not valid
                        break
                    }
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun stopPositionUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    fun stopPlaybackAndNotification() {
        Log.d("MusicPlayerService", "stopPlaybackAndNotification called")
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset() // Reset to uninitialized state
        }
        // Do not release mediaPlayer here, initializeMediaPlayer will handle it or onDestroy

        stopPositionUpdates()
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _currentSong.value = null // This will trigger observers to clear UI
        currentSongIndex = -1
        currentQueue = emptyList()

        mediaSession.isActive = false
        stopForegroundAndRemoveNotification()
        Log.d("MusicPlayerService", "Service playback stopped and notification removed.")
    }

    private fun stopForegroundAndRemoveNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel(NOTIFICATION_ID) // Ensure notification is removed
    }


    // Resets player state but keeps service alive and queue intact
    fun resetPlayerState() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
        }
        // Re-initialize for next playback if needed, or rely on playSong
        initializeMediaPlayer() // Get it ready for a new song.

        stopPositionUpdates()
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        // Don't clear _currentSong here, as it might be needed to replay or show info
        // If current song should be cleared, call stopPlaybackAndNotification

        updateMediaSessionStateAndNotification() // Update UI to reflect reset state
    }


    private fun releaseMediaPlayer() {
        stopPositionUpdates()
        mediaPlayer?.release()
        mediaPlayer = null

        _isPlaying.value = false // Reset states
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    override fun onDestroy() {
        Log.d("MusicPlayerService", "onDestroy called")
        releaseMediaPlayer()
        mediaSession.release()
        serviceScope.cancel() // Cancel all coroutines started in this scope
        super.onDestroy()
    }

    private fun updateMediaSessionStateAndNotification() {
        val songToUpdateWith = _currentSong.value // Capture current state at call time
        val isPlayingUpdate = _isPlaying.value
        val currentPosUpdate = _currentPosition.value
        val durationUpdate = _duration.value

        // Update PlaybackState immediately (fast operation)
        updateMediaSessionPlaybackState(songToUpdateWith, currentPosUpdate, isPlayingUpdate)

        // Launch a coroutine for operations that might involve IO (like fetching album art)
        serviceScope.launch {
            val albumArtBitmap = songToUpdateWith?.songArtUri?.let { uri ->
                loadAndProcessBitmap(applicationContext, uri, NOTIFICATION_ART_SIZE, NOTIFICATION_ART_SIZE)
            }
            // It's possible songToUpdateWith is null if currentSong was cleared rapidly.
            // The functions below handle null song.

            // Update metadata (includes album art)
            updateMediaSessionMetadataInternal(songToUpdateWith, albumArtBitmap, durationUpdate)

            if (songToUpdateWith != null) {
                val notification = buildNotificationInternal(
                    songToUpdateWith,
                    isPlayingUpdate,
                    albumArtBitmap,
                    isOngoing = true // Media notifications are typically ongoing
                )
                if (isPlayingUpdate) {
                    startForeground(NOTIFICATION_ID, notification)
                } else if (mediaSession.isActive) {
                    // Paused, but session is active (e.g., ready to resume)
                    // Update the notification, keeping it dismissable=false (ongoing)
                    // If service was already foreground, this updates it.
                    // If not yet foreground, this posts a sticky notification.
                    // To ensure it becomes foreground even if paused initially:
                    // startForeground(NOTIFICATION_ID, notification)
                    // However, original logic was to use notify:
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } else {
                    // Not playing and media session not active, or no song.
                    stopForegroundAndRemoveNotification()
                }
            } else {
                // No song, remove notification and stop foreground state.
                stopForegroundAndRemoveNotification()
            }
        }
    }

    private fun updateMediaSessionPlaybackState(song: Song?, position: Long, isPlaying: Boolean) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                position,
                1.0f // Playback speed
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadataInternal(song: Song?, albumArt: Bitmap?, actualMediaPlayerDuration: Long) {
        if (song == null) {
            mediaSession.setMetadata(null)
            return
        }

        val durationToUse = if (actualMediaPlayerDuration > 0 && song.id == _currentSong.value?.id) {
            actualMediaPlayerDuration // Prefer actual duration from MediaPlayer if current song
        } else {
            song.duration // Fallback to song's stored duration
        }

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationToUse)

        // For metadata, we might want a higher resolution image if available
        // This example uses the same bitmap as notification, but you could load a different one.
        // For simplicity, reusing the one loaded for notification or loading a new one for metadata.
        // The current `albumArt` is sized for notification. If you need a different one for metadata:
        // val metadataArtBitmap = serviceScope.async(Dispatchers.IO) { loadAndProcessBitmap(...) }.await()
        // For this example, we'll use the provided albumArt (sized for notification).
        albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }
        // Optionally, load a separate, higher-res bitmap for metadata here if needed
        // and if albumArt was specifically for notification.

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun buildNotificationInternal(
        song: Song,
        isPlaying: Boolean,
        albumArt: Bitmap?,
        isOngoing: Boolean = true
    ): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply { // Replace MainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Or your preferred flags
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Intent for when notification is dismissed by swipe (if not ongoing, or for deleteIntent)
        val stopServiceIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = ACTION_STOP_FOREGROUND
        }
        val deletePendingIntent = PendingIntent.getService(
            this, 0, stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // YOUR SMALL ICON (often monochrome)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent) // Called when notification is dismissed
            .setOngoing(isOngoing) // Makes it non-swipeable if true
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen

        val largeIconBitmap = albumArt ?: BitmapFactory.decodeResource(resources, R.drawable.dummy_song_art)
        builder.setLargeIcon(largeIconBitmap)

        // Previous Action
        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        builder.addAction(R.drawable.ic_skip_previous_white, "Previous", prevIntent)

        // Play/Pause Action
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_arrow_white
        val playPauseActionText = if (isPlaying) "Pause" else "Play"
        val playPauseAction = if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, playPauseAction)
        builder.addAction(playPauseIcon, playPauseActionText, playPauseIntent)

        // Next Action
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        builder.addAction(R.drawable.ic_skip_next_white, "Next", nextIntent)

        // Apply MediaStyle
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        builder.setStyle(mediaStyle)

        return builder.build()
    }

    private suspend fun loadAndProcessBitmap(context: Context, uriString: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(uriString)
                    .override(reqWidth, reqHeight)
                    .centerCrop()
                    .error(R.drawable.dummy_song_art)
                    .submit()
                    .get()
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "Error loading bitmap from URI: $uriString", e)
                null
            }
        }
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }
}