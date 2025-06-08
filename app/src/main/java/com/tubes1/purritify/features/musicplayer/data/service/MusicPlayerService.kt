package com.tubes1.purritify.features.musicplayer.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
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
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.tubes1.purritify.MainActivity
import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import com.tubes1.purritify.R
import com.tubes1.purritify.core.data.local.dao.PlayHistoryDao
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDeviceSource
import com.tubes1.purritify.features.audiorouting.domain.model.DeviceType
import com.tubes1.purritify.features.audiorouting.domain.model.PairingStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import java.io.IOException


class MusicPlayerService : Service(), KoinComponent {
    private val binder = MusicPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    var currentQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = -1
    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    private var playHistoryDao: PlayHistoryDao? = null

    private val _preferredAudioDevice = MutableStateFlow<AudioDevice?>(null)
    val preferredAudioDevice: StateFlow<AudioDevice?> = _preferredAudioDevice.asStateFlow()

    private lateinit var audioManager: AudioManager

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    sealed class MusicServiceEvent {
        data class AudioOutputChanged(val message: String, val newDeviceName: String?) : MusicServiceEvent()
        data class PlaybackPausedDueToNoise(val message: String) : MusicServiceEvent()
    }

    private val _serviceEvents = MutableSharedFlow<MusicServiceEvent>(replay = 0)
    val serviceEvents: SharedFlow<MusicServiceEvent> = _serviceEvents.asSharedFlow()

    private var audioBecomingNoisyReceiver: BroadcastReceiver? = null
    private var bluetoothA2dpStateReceiver: BroadcastReceiver? = null

    private var currentSongPlayStartTime: Long = 0L
    private var currentSongListenedDurationBeforePause: Long = 0L

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "purritify_music_channel"
        const val NOTIFICATION_ID = 1337
        const val ACTION_PLAY = "com.tubes1.purritify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.tubes1.purritify.ACTION_PAUSE"
        const val ACTION_NEXT = "com.tubes1.purritify.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.tubes1.purritify.ACTION_PREVIOUS"
        const val ACTION_STOP_FOREGROUND = "com.tubes1.purritify.ACTION_STOP_FOREGROUND"
        const val ACTION_OPEN_PLAYER = "com.tubes1.purritify.ACTION_OPEN_PLAYER"

        private const val NOTIFICATION_ART_SIZE = 256
        private const val TAG = "MusicPlayerService"
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service creating.")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSessionCompat(this, "PurritifyMediaSession").apply {
            setCallback(mediaSessionCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }

        initializeMediaPlayer()
        observePlayerStateForNotification()
        registerAudioDeviceReceivers()
        Log.d(TAG, "onCreate: Service created and initializations done.")
    }

    fun setPlayHistoryDao(dao: PlayHistoryDao) {
        this.playHistoryDao = dao
        Log.i(TAG, "PlayHistoryDao has been set in service.")
    }

    fun updatePreferredAudioDevice(device: AudioDevice?) {
        val oldDeviceName = _preferredAudioDevice.value?.name
        Log.i(TAG, "Service updating preferred audio device to: ${device?.name ?: "System Default"}")
        _preferredAudioDevice.value = device
        applyAudioRouting()

        if (oldDeviceName != device?.name) {
            sendServiceEvent(MusicServiceEvent.AudioOutputChanged(
                "Audio output will now use ${device?.name ?: "System Default"}.",
                device?.name?.toString()
            ))
        }
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
            setOnCompletionListener {
                Log.d(TAG, "Song completed.")
                val completedSong = _currentSong.value
                if (completedSong != null) {
                    val elapsedTime = System.currentTimeMillis() - currentSongPlayStartTime
                    currentSongListenedDurationBeforePause += elapsedTime
                    logPlayHistoryIfNeeded(isCompletion = true, manualStop = false, listenedDuration = currentSongListenedDurationBeforePause)
                }
                currentSongListenedDurationBeforePause = 0L
                playNext()
            }
            setOnPreparedListener { mp ->
                Log.d(TAG, "MediaPlayer prepared. Duration: ${mp.duration}")
                _duration.value = mp.duration.toLong()

                applyAudioRouting()
                mp.start()
                _isPlaying.value = true
                currentSongPlayStartTime = System.currentTimeMillis()
                currentSongListenedDurationBeforePause = 0L
                startPositionUpdates()
                mediaSession.isActive = true

                val currentSong = _currentSong.value

                serviceScope.launch {
                    val albumArtBitmap = currentSong?.songArtUri?.let { uri ->
                        loadAndProcessBitmap(applicationContext, uri, NOTIFICATION_ART_SIZE, NOTIFICATION_ART_SIZE)
                    }
                    updateMediaSessionPlaybackState(currentSong, 0L, true)
                    updateMediaSessionMetadataInternal(currentSong, albumArtBitmap, mp.duration.toLong())

                    if (currentSong != null) {
                        val notification = buildNotificationInternal(
                            currentSong,
                            isPlaying = true,
                            albumArt = albumArtBitmap,
                            isOngoing = true
                        )
                        startForeground(NOTIFICATION_ID, notification)
                        Log.d(TAG, "MediaPlayer started and foreground service initiated.")
                    } else {
                        Log.w(TAG, "MediaPlayer prepared but currentSong is null. Cannot start foreground.")
                    }
                }
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer Error: What: $what, Extra: $extra")
                stopPositionUpdates()
                _isPlaying.value = false
                updateMediaSessionStateAndNotification()
                true
            }
        }
        Log.d(TAG, "MediaPlayer initialized.")
    }

    private fun applyAudioRouting() {
        val player = mediaPlayer ?: return
        val preferredDev = _preferredAudioDevice.value

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var targetSystemDevice: AudioDeviceInfo? = null

            if (preferredDev != null) {
                Log.d(TAG, "Applying routing based on user preference: Name=${preferredDev.name}, " +
                        "SystemAPI ID=${preferredDev.systemApiId}, Address=${preferredDev.address}, " +
                        "AppType=${preferredDev.type}, SystemDeviceType=${preferredDev.systemDeviceType}")

                val systemDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                Log.d(TAG, "Available system output devices for routing consideration (${systemDevices.size} total):")
                systemDevices.forEach { devInfo ->
                    Log.d(TAG, "  SysDev: Name=${devInfo.productName}, ID=${devInfo.id}, Type=${devInfo.type}, Address=${devInfo.address}")
                }

                if (preferredDev.address != null &&
                    (preferredDev.type == DeviceType.BLUETOOTH_A2DP ||
                            preferredDev.type == DeviceType.BLUETOOTH_SCO ||
                            preferredDev.type == DeviceType.HEARING_AID)) {

                    val matchingAddressDevices = systemDevices.filter { it.address == preferredDev.address }

                    if (matchingAddressDevices.isNotEmpty()) {
                        Log.d(TAG, "Found ${matchingAddressDevices.size} system device(s) with address: ${preferredDev.address}")

                        targetSystemDevice = matchingAddressDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
                        if (targetSystemDevice != null) {
                            Log.d(TAG, "Match by address - A2DP preferred: ID=${targetSystemDevice!!.id}, Name=${targetSystemDevice!!.productName}")
                        } else {
                            targetSystemDevice = matchingAddressDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                            if (targetSystemDevice != null) {
                                Log.d(TAG, "Match by address - SCO: ID=${targetSystemDevice!!.id}, Name=${targetSystemDevice!!.productName}")
                            } else {
                                targetSystemDevice = matchingAddressDevices.firstOrNull()
                                if (targetSystemDevice != null) {
                                    Log.d(TAG, "Match by address - Other BT type (${targetSystemDevice!!.type}): ID=${targetSystemDevice!!.id}, Name=${targetSystemDevice!!.productName}")
                                }
                            }
                        }
                    }
                }

                if (targetSystemDevice == null && preferredDev.systemApiId != null) {
                    targetSystemDevice = systemDevices.find { it.id == preferredDev.systemApiId }
                    if (targetSystemDevice != null) {
                        Log.d(TAG, "Match by systemApiId: ID=${targetSystemDevice!!.id}, Name=${targetSystemDevice!!.productName}, Type=${targetSystemDevice!!.type}")
                    }
                }

                if (targetSystemDevice == null) {
                    targetSystemDevice = systemDevices.find {
                        it.type == preferredDev.systemDeviceType && it.productName == preferredDev.name
                    }
                    if (targetSystemDevice != null) {
                        Log.d(TAG, "Match by type (${targetSystemDevice!!.type}) and name (${targetSystemDevice!!.productName}): ID=${targetSystemDevice!!.id}")
                    }
                }
            } else {
                Log.d(TAG, "No user preference. Using system default routing (targetSystemDevice = null).")

            }
            if (preferredDev != null) {
                if (targetSystemDevice != null) {
                    Log.i(TAG, "Final targetSystemDevice chosen: Name=${targetSystemDevice.productName}, ID=${targetSystemDevice.id}, Type=${targetSystemDevice.type}")
                } else {
                    Log.w(TAG, "Preferred device '${preferredDev.name}' NOT FOUND in available system devices. Routing to default.")
                }
            }

            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                    val success = player.setPreferredDevice(targetSystemDevice)
                    Log.i(TAG, "MediaPlayer.setPreferredDevice(${targetSystemDevice?.productName ?: "DEFAULT"}, ID: ${targetSystemDevice?.id}) success: $success")

                    if (!success && targetSystemDevice != null &&
                        (targetSystemDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || targetSystemDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                        Log.w(TAG, "Failed to set preferred BT device '${targetSystemDevice.productName}'. Attempting to route to system default explicitly.")
                        val defaultSuccess = player.setPreferredDevice(null)
                        Log.i(TAG, "Attempt to route to system default explicitly (after BT fail) success: $defaultSuccess")
                    }

                } else {
                    Log.w(TAG, "MODIFY_AUDIO_SETTINGS permission not granted. Using system default.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting audio device info: ${e.message}", e)

                try {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                        player.setPreferredDevice(null)
                        Log.i(TAG, "Fell back to system default due to exception during setPreferredDevice.")
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error falling back to system default after exception: ${ex.message}", ex)
                }
            }
        } else {
            Log.d(TAG, "Audio routing via setPreferredDevice not available on API < 28.")
        }
    }


    private fun registerAudioDeviceReceivers() {
        Log.d(TAG, "Registering audio device receivers...")

        if (audioBecomingNoisyReceiver == null) {
            audioBecomingNoisyReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                        Log.i(TAG, "ACTION_AUDIO_BECOMING_NOISY received. Pausing playback.")
                        pausePlaybackInternal()
                        sendServiceEvent(MusicServiceEvent.PlaybackPausedDueToNoise(
                            "Audio output changed, playback paused."
                        ))
                    }
                }
            }
            registerReceiver(audioBecomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            Log.d(TAG, "Registered ACTION_AUDIO_BECOMING_NOISY receiver.")
        }

        if (bluetoothA2dpStateReceiver == null) {
            val canListenBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }

            if (!canListenBluetooth) {
                Log.w(TAG, "Required Bluetooth permission for A2DP state changes is missing. Cannot register A2DP receiver.")
                return
            }

            bluetoothA2dpStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == action) {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                        val previousState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, BluetoothProfile.STATE_DISCONNECTED)

                        var deviceNameForLog = "Unknown Device"
                        if (device != null) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    deviceNameForLog = device.name ?: "Unnamed BT Device"
                                } else {
                                    Log.w(TAG, "BLUETOOTH_CONNECT permission missing, cannot get device name for A2DP state change.")
                                    deviceNameForLog = "BT Device (name perm missing)"
                                }
                            } else {
                                @Suppress("MissingPermission")
                                deviceNameForLog = device.name ?: "Unnamed BT Device"
                            }
                        }

                        Log.i(TAG, "A2DP Connection State Changed: Device: $deviceNameForLog, Address: ${device?.address}, " +
                                "Previous State: $previousState -> New State: $state")

                        val currentServicePreferredDev = _preferredAudioDevice.value

                        if (device != null && currentServicePreferredDev?.address == device.address) {
                            if (state == BluetoothProfile.STATE_DISCONNECTED && previousState != BluetoothProfile.STATE_DISCONNECTED) {
                                Log.w(TAG, "Preferred audio device '$deviceNameForLog' has disconnected.")

                                val speakerForPref = getSpeakerAsAudioDevice()
                                if (speakerForPref != null) {
                                    Log.i(TAG, "Routing audio to internal speaker due to preferred device disconnection.")
                                    updatePreferredAudioDevice(speakerForPref)
                                } else {
                                    Log.w(TAG, "Could not find internal speaker info. Falling back to system default.")
                                    updatePreferredAudioDevice(null)
                                }
                            } else if (state == BluetoothProfile.STATE_CONNECTED && previousState == BluetoothProfile.STATE_CONNECTING) {
                                Log.i(TAG, "Preferred audio device '$deviceNameForLog' has reconnected/connected.")
                                updatePreferredAudioDevice(currentServicePreferredDev)
                            }
                        } else if (device != null && state == BluetoothProfile.STATE_CONNECTED && currentServicePreferredDev?.type != DeviceType.BLUETOOTH_A2DP) {
                            Log.i(TAG, "A Bluetooth device '$deviceNameForLog' connected, but it's not the current active service preference or preferred was not BT.")
                        }
                    }
                }
            }
            val a2dpFilter = IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothA2dpStateReceiver, a2dpFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(bluetoothA2dpStateReceiver, a2dpFilter)
            }
            Log.d(TAG, "Registered A2DP connection state receiver.")
        }
    }

    private fun unregisterAudioDeviceReceivers() {
        Log.d(TAG, "Unregistering audio device receivers...")
        try {
            audioBecomingNoisyReceiver?.let { unregisterReceiver(it); audioBecomingNoisyReceiver = null }
            Log.d(TAG, "Unregistered ACTION_AUDIO_BECOMING_NOISY receiver.")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "ACTION_AUDIO_BECOMING_NOISY receiver already unregistered or not registered: ${e.message}")
        }
        try {
            bluetoothA2dpStateReceiver?.let { unregisterReceiver(it); bluetoothA2dpStateReceiver = null }
            Log.d(TAG, "Unregistered A2DP connection state receiver.")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "A2DP receiver already unregistered or not registered: ${e.message}")
        }
    }

    private fun getSpeakerAsAudioDevice(): AudioDevice? {
        val speakerInfo = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        return speakerInfo?.let {
            AudioDevice(
                systemApiId = it.id,
                name = it.productName?.toString() ?: "Device Speaker",
                type = DeviceType.BUILTIN_SPEAKER,
                systemDeviceType = it.type,
                address = null,
                source = AudioDeviceSource.SYSTEM_API,
                isCurrentlySelectedOutput = false,
                pairingStatus = PairingStatus.PAIRED
            )
        }
    }

    private fun sendServiceEvent(event: MusicServiceEvent) {
        serviceScope.launch {
            _serviceEvents.emit(event)
            Log.d(TAG, "Sent service event: $event")
        }
    }

    private fun pausePlaybackInternal() {
        if (mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "pausePlaybackInternal: Pausing media player.")
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopPositionUpdates()
            updateMediaSessionStateAndNotification()
        } else {
            Log.d(TAG, "pausePlaybackInternal: Player not playing or null.")
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
        Log.d(TAG, "playSong called for: ${song.title}")

        val previouslyPlayingSong = _currentSong.value
        if (previouslyPlayingSong != null && previouslyPlayingSong.id != song.id && _isPlaying.value) {
            val playedDuration = System.currentTimeMillis() - currentSongPlayStartTime
            currentSongListenedDurationBeforePause += playedDuration
            Log.d(TAG, "Song changed while playing ${previouslyPlayingSong.title}. Logging its play duration: ${currentSongListenedDurationBeforePause}ms")
            logPlayHistoryIfNeeded(isCompletion = false, manualStop = true, listenedDuration = currentSongListenedDurationBeforePause)
        }
        currentSongListenedDurationBeforePause = 0L

        _currentSong.value = song
        currentQueue = queue
        currentSongIndex = queue.indexOf(song).takeIf { it >= 0 } ?: 0

        if (mediaPlayer != null) {
            try {
                Log.d(TAG, "playSong: Attempting to reset existing MediaPlayer.")
                mediaPlayer?.reset()
                Log.d(TAG, "playSong: MediaPlayer reset successfully.")
            } catch (e: IllegalStateException) {
                Log.w(TAG, "playSong: MediaPlayer reset failed (${e.message}), re-initializing.")
                initializeMediaPlayer()
            } catch (e: Exception) {
                Log.e(TAG, "playSong: Unexpected error during MediaPlayer reset (${e.message}), re-initializing.", e)
                initializeMediaPlayer()
            }
        } else {
            Log.d(TAG, "playSong: MediaPlayer is null, initializing a new one.")
            initializeMediaPlayer()
        }

        mediaPlayer?.apply {
            try {
                Log.d(TAG, "playSong: Setting data source to: ${song.path}")
                if (song.path.startsWith("http://") || song.path.startsWith("https://")) {
                    setDataSource(song.path)
                } else {
                    setDataSource(applicationContext, Uri.parse(song.path))
                }
                Log.d(TAG, "playSong: Data source set. Calling prepareAsync.")
                prepareAsync()
            } catch (e: IOException) {
                Log.e(TAG, "IOException setting data source for ${song.path}", e)
                handlePlaybackError()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException setting data source for ${song.path} (should not happen often): ${e.message}", e)
                handlePlaybackError()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException setting data source for ${song.path}. Check URI permissions.", e)
                handlePlaybackError()
            } catch (e: Exception) {
                Log.e(TAG, "Generic exception during MediaPlayer setup for ${song.path}: ${e.message}", e)
                handlePlaybackError()
            }
        } ?: run {
            Log.e(TAG, "playSong: MediaPlayer is still null after attempting to initialize/reset. This is a critical error.")
            handlePlaybackError()
        }
    }

    private fun handlePlaybackError() {
        _isPlaying.value = false
        _currentSong.value = null
        currentSongIndex = -1
        updateMediaSessionStateAndNotification()

    }

    fun playPause() {
        mediaPlayer?.let {
            if (_isPlaying.value) {
                Log.d(TAG, "playPause: Pausing.")
                it.pause()
                _isPlaying.value = false
                val playedDuration = System.currentTimeMillis() - currentSongPlayStartTime
                currentSongListenedDurationBeforePause += playedDuration
                logPlayHistoryIfNeeded(isCompletion = false, manualStop = true, listenedDuration = currentSongListenedDurationBeforePause)
                stopPositionUpdates()
            } else {
                Log.d(TAG, "playPause: Playing.")
                try {
                    if (_currentSong.value == null && currentQueue.isNotEmpty() && currentSongIndex in currentQueue.indices) {

                        Log.d(TAG, "playPause: No current song in player, attempting to play from queue index $currentSongIndex")
                        playSong(currentQueue[currentSongIndex], currentQueue)
                        return
                    }

                    it.start()
                    _isPlaying.value = true
                    currentSongPlayStartTime = System.currentTimeMillis()
                    startPositionUpdates()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "playPause: MediaPlayer not ready to start, attempting to replay current song.", e)
                    _currentSong.value?.let { current -> playSong(current, currentQueue) }
                        ?: Log.e(TAG, "playPause: No current song to replay on error.")
                }
            }
            updateMediaSessionStateAndNotification()
        } ?: run {
            if (currentQueue.isNotEmpty() && currentSongIndex != -1 && currentSongIndex in currentQueue.indices) {
                Log.w(TAG, "playPause: MediaPlayer is null, attempting to play song at index $currentSongIndex")
                playSong(currentQueue[currentSongIndex], currentQueue)
            } else {
                Log.w("MusicPlayerService", "playPause: No media player and no valid song in queue.")
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

                        val liveElapsed = (System.currentTimeMillis() - currentSongPlayStartTime) + currentSongListenedDurationBeforePause
                        _liveElapsedTimeMs.value = liveElapsed

                        updateMediaSessionPlaybackState(_currentSong.value, currentPos, _isPlaying.value)
                    } catch (e: IllegalStateException) {
                        Log.w("MusicPlayerService", "Error getting current position: ${e.message}")
                        stopPositionUpdates()
                        break
                    }
                } else {
                    _liveElapsedTimeMs.value = currentSongListenedDurationBeforePause
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
        Log.d(TAG, "stopPlaybackAndNotification called")

        if (_isPlaying.value && _currentSong.value != null) {
            val playedDuration = System.currentTimeMillis() - currentSongPlayStartTime
            currentSongListenedDurationBeforePause += playedDuration
            logPlayHistoryIfNeeded(isCompletion = false, manualStop = true, listenedDuration = currentSongListenedDurationBeforePause)
        }
        currentSongListenedDurationBeforePause = 0L

        mediaPlayer?.apply {
            if (this.isPlaying) {
                try { this.stop() }
                catch (e: IllegalStateException) { Log.w(TAG, "MediaPlayer.stop() failed: ${e.message}") }
            }
            try { this.reset() }
            catch (e: IllegalStateException) { Log.w(TAG, "MediaPlayer.reset() failed: ${e.message}") }
        }

        stopPositionUpdates()
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _currentSong.value = null
        currentSongIndex = -1
        currentQueue = emptyList()

        mediaSession.isActive = false
        updateMediaSessionStateAndNotification()

        Log.d(TAG, "Service playback stopped.")
    }

    private fun logPlayHistoryIfNeeded(isCompletion: Boolean, manualStop: Boolean, listenedDuration: Long) {
        val songToLog = _currentSong.value
        val currentPlayHistoryDao = playHistoryDao

        if (currentPlayHistoryDao == null) {
            Log.e(TAG, "PlayHistoryDao is null in service. Cannot log play history.")
            return
        }

        if (songToLog?.id == null) {
            Log.w(TAG, "Cannot log play history, song or songId is null.")
            return
        }

        val significantPlayThresholdMs = 10000L
        if (!isCompletion && listenedDuration < significantPlayThresholdMs) {
            Log.d(TAG, "Song '${songToLog.title}' listened for ${listenedDuration}ms, less than threshold. Not logging.")
            return
        }

        val actualListenedDuration = if (isCompletion) songToLog.duration else listenedDuration

        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val monthInt = calendar.get(java.util.Calendar.MONTH) + 1
        val monthYear = String.format("%d-%02d", year, monthInt)

        val historyEntry = PlayHistoryEntity(
            datetime = System.currentTimeMillis(),
            songId = songToLog.id!!,
            artist = songToLog.artist,
            month = monthYear,
            duration = actualListenedDuration
        )

        serviceScope.launch(Dispatchers.IO) {
            try {
                val id = currentPlayHistoryDao.insertPlayHistory(historyEntry)
                Log.i(TAG, "Logged play history for '${songToLog.title}', ID: $id, Duration: $actualListenedDuration, Month: $monthYear")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging play history for ${songToLog.title}", e)
            }
        }
    }

    private val _liveElapsedTimeMs = MutableStateFlow(0L)
    val liveElapsedTimeMs: StateFlow<Long> = _liveElapsedTimeMs.asStateFlow()

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
        Log.d(TAG, "onDestroy called")
        unregisterAudioDeviceReceivers()
        stopPlaybackAndNotification()
        releaseMediaPlayer()
        mediaSession.release()
        serviceScope.cancel()
        notificationManager.cancelAll()
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroyed and resources released.")
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