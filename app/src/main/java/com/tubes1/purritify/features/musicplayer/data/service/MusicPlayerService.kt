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
        const val ACTION_OPEN_PLAYER = "com.tubes1.purritify.ACTION_OPEN_PLAYER"

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
                mediaSession.isActive = true

                val currentSong = _currentSong.value
                val albumArtBitmap = currentSong?.songArtUri?.let { uri ->
                    loadAndProcessBitmapBlocking(applicationContext, uri, NOTIFICATION_ART_SIZE, NOTIFICATION_ART_SIZE)
                }

                updateMediaSessionPlaybackState(currentSong, 0L, true)
                updateMediaSessionMetadataInternal(currentSong, albumArtBitmap, mp.duration.toLong())

                val notification = buildNotificationInternal(
                    currentSong!!,
                    isPlaying = true,
                    albumArt = albumArtBitmap,
                    isOngoing = true
                )

                startForeground(NOTIFICATION_ID, notification)
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerService", "MediaPlayer Error: What: $what, Extra: $extra")
                stopPositionUpdates()
                _isPlaying.value = false
                updateMediaSessionStateAndNotification()
                true
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicPlayerService", "onStartCommand received action: ${intent?.action}")
        val action = intent?.action
        if (action.isNullOrEmpty() && currentSong.value == null && intent?.extras == null) {
            Log.d("MusicPlayerService", "Service restarted sticky, no current song, stopping self.")
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_PLAY -> playPause()
            ACTION_PAUSE -> playPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP_FOREGROUND -> {
                Log.i("MusicPlayerService", "ACTION_STOP_FOREGROUND received from onStartCommand.")
                stopPlaybackAndNotification()
            }
            else -> {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setSound(null, null)
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
            stopPlaybackAndNotification()
        }

        override fun onSeekTo(pos: Long) {
            Log.d("MediaSessionCallback", "onSeekTo: $pos")
            seekTo(pos)
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        _currentSong.value = song
        currentQueue = queue
        currentSongIndex = queue.indexOf(song).takeIf { it >= 0 } ?: 0

        initializeMediaPlayer()

        mediaPlayer?.apply {
            try {
                setDataSource(applicationContext, Uri.parse(song.path))
                prepareAsync()
            } catch (e: IOException) {
                Log.e("MusicPlayerService", "Error setting data source for ${song.path}", e)
                _isPlaying.value = false
                _currentSong.value = null
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
            if (_isPlaying.value) {
                it.pause()
                _isPlaying.value = false
                stopPositionUpdates()
            } else {
                try {
                    it.start()
                    _isPlaying.value = true
                    startPositionUpdates()
                } catch (e: IllegalStateException) {
                    Log.e("MusicPlayerService", "MediaPlayer not ready to start, attempting to replay current song.", e)
                    currentSong.value?.let { current -> playSong(current, currentQueue) }
                        ?: Log.e("MusicPlayerService", "No current song to replay.")
                }
            }
            updateMediaSessionStateAndNotification()
        } ?: run {
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

        if ((mediaPlayer?.currentPosition ?: 0) > 3000 && _isPlaying.value) {
            seekTo(0)
            if (!_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startPositionUpdates()
                updateMediaSessionStateAndNotification()
            }
            return
        }

        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else currentQueue.size - 1
        if (currentSongIndex < 0 && currentQueue.isNotEmpty()) currentSongIndex = 0

        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex], currentQueue)
        }
    }


    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
        updateMediaSessionPlaybackState(_currentSong.value, position, _isPlaying.value)
    }

    private fun startPositionUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        val currentPos = mediaPlayer!!.currentPosition.toLong()
                        _currentPosition.value = currentPos
                        updateMediaSessionPlaybackState(_currentSong.value, currentPos, _isPlaying.value)
                    } catch (e: IllegalStateException) {
                        Log.w("MusicPlayerService", "Error getting current position: ${e.message}")
                        stopPositionUpdates()
                        break
                    }
                }
                delay(1000)
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
            if (this.isPlaying) {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    Log.w("MusicPlayerService", "MediaPlayer.stop() failed: ${e.message}")
                }
            }
            try {
                reset()
            } catch (e: IllegalStateException) {
                Log.w("MusicPlayerService", "MediaPlayer.reset() failed: ${e.message}")
            }
        }

        stopPositionUpdates()
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L

        mediaSession.isActive = false
        updateMediaSessionStateAndNotification()

        Log.d("MusicPlayerService", "Service playback stopped and notification removed.")
    }

    private fun stopForegroundAndRemoveNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun releaseMediaPlayer() {
        stopPositionUpdates()
        mediaPlayer?.release()
        mediaPlayer = null

        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    override fun onDestroy() {
        Log.d("MusicPlayerService", "onDestroy called")
        stopPlaybackAndNotification()
        releaseMediaPlayer()
        mediaSession.release()
        serviceScope.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun updateMediaSessionStateAndNotification() {
        val songToUpdateWith = _currentSong.value
        val isPlayingUpdate = _isPlaying.value
        val currentPosUpdate = _currentPosition.value
        val durationUpdate = _duration.value

        updateMediaSessionPlaybackState(songToUpdateWith, currentPosUpdate, isPlayingUpdate)

        serviceScope.launch {
            val albumArtBitmap = songToUpdateWith?.songArtUri?.let { uri ->
                loadAndProcessBitmap(applicationContext, uri, NOTIFICATION_ART_SIZE, NOTIFICATION_ART_SIZE)
            }

            updateMediaSessionMetadataInternal(songToUpdateWith, albumArtBitmap, durationUpdate)

            if (songToUpdateWith != null && mediaSession.isActive) {
                val notification = buildNotificationInternal(
                    songToUpdateWith,
                    isPlayingUpdate,
                    albumArtBitmap,
                    isOngoing = true
                )
                startForeground(NOTIFICATION_ID, notification)
            } else {
                Log.d("MusicPlayerService", "Stopping foreground. Song: ${songToUpdateWith?.title}, Active: ${mediaSession.isActive}, isPlaying: $isPlayingUpdate")
                stopForegroundAndRemoveNotification()

                if (!mediaSession.isActive) {
                    Log.d("MusicPlayerService", "MediaSession not active, clearing current song from service state.")
                    _currentSong.value = null
                    currentSongIndex = -1
                    currentQueue = emptyList()
                    _duration.value = 0L
                }
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
                1.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadataInternal(song: Song?, albumArt: Bitmap?, actualMediaPlayerDuration: Long) {
        if (song == null) {
            mediaSession.setMetadata(null)
            return
        }

        val durationToUse = if (actualMediaPlayerDuration > 0 && song.id == _currentSong.value?.id) {
            actualMediaPlayerDuration
        } else {
            song.duration
        }

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationToUse)

        albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun buildNotificationInternal(
        song: Song,
        isPlaying: Boolean,
        albumArt: Bitmap?,
        isOngoing: Boolean = true
    ): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopServiceIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = ACTION_STOP_FOREGROUND
        }
        val deletePendingIntent = PendingIntent.getService(
            this, 1, stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
//            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setAutoCancel(false)
            .setOngoing(isOngoing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val largeIconBitmap = albumArt ?: BitmapFactory.decodeResource(resources, R.drawable.dummy_song_art)
        builder.setLargeIcon(largeIconBitmap)

        val prevIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        builder.addAction(R.drawable.ic_skip_previous_white, "Previous", prevIntent)

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_arrow_white
        val playPauseActionText = if (isPlaying) "Pause" else "Play"
        val playPauseAction = if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, playPauseAction)
        builder.addAction(playPauseIcon, playPauseActionText, playPauseIntent)

        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        builder.addAction(R.drawable.ic_skip_next_white, "Next", nextIntent)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
//            .setShowCancelButton(true)
//            .setCancelButtonIntent(stopMediaIntent)

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

    private fun loadAndProcessBitmapBlocking(context: Context, uriString: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("MusicPlayerService", "onTaskRemoved called. App swiped from recents.")
        super.onTaskRemoved(rootIntent)
        stopPlaybackAndNotification()
        stopSelf()
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }
}