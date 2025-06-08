package com.tubes1.purritify.features.musicplayer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.tubes1.purritify.core.data.local.dao.PlayHistoryDao
import com.tubes1.purritify.core.data.local.preferences.UserPreferencesRepository
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService
import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import com.tubes1.purritify.features.soundcapsule.domain.usecase.GetCurrentMonthTimeListenedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MusicPlayerRepositoryImpl(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val playHistoryDao: PlayHistoryDao
) : MusicPlayerRepository, GetCurrentMonthTimeListenedUseCase.MusicPlayerServiceFlows {

    private var musicService: MusicPlayerService? = null
    private val _serviceBoundFlow = MutableStateFlow(false)
    val serviceBoundFlow: StateFlow<Boolean> = _serviceBoundFlow.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("MusicPlayerRepo", "Service connected: $name")
            val binder = service as MusicPlayerService.MusicPlayerBinder
            musicService = binder.getService()
            _serviceBoundFlow.value = true

            musicService?.setPlayHistoryDao(playHistoryDao)

            repositoryScope.launch {
                userPreferencesRepository.preferredAudioDeviceFlow.firstOrNull()?.let { device ->
                    musicService?.updatePreferredAudioDevice(device)
                }
            }

            observeServiceEvents()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("MusicPlayerRepo", "Service disconnected: $name")
            musicService = null
            _serviceBoundFlow.value = false
        }
    }

    init {
        Log.d("MusicPlayerRepo", "Initializing and binding service.")
        bindToService()
    }

    private fun bindToService() {
        Intent(context, MusicPlayerService::class.java).also { serviceIntent ->
            try {
                context.startService(serviceIntent)
                Log.d("MusicPlayerRepo", "context.startService called.")
            } catch (e: Exception) {
                Log.e("MusicPlayerRepo", "Error calling startService: ${e.message}", e)
            }
        }

        Intent(context, MusicPlayerService::class.java).also { bindIntent ->
            val success = context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("MusicPlayerRepo", "bindService called, success: $success")
            if (!success && !_serviceBoundFlow.value) {
                Log.e("MusicPlayerRepo", "Immediate bindService FAILED. Service might not be available.")
            }
        }
    }

    override fun getPlayerState(): Flow<MusicPlayerState> {
        return serviceBoundFlow.flatMapLatest { isBound ->
            if (isBound && musicService != null) {
                val currentService = musicService!!
                combine(
                    currentService.currentSong,
                    currentService.isPlaying,
                    currentService.currentPosition,
                    currentService.duration,
                    currentService.preferredAudioDevice
                ) { song, isPlaying, position, duration, preferredDevice ->
                    MusicPlayerState(
                        currentSong = song,
                        queue = currentService.currentQueue,
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,
                        activeAudioDevice = preferredDevice
                    )
                }
            } else {
                flowOf(MusicPlayerState(queue = emptyList()))
            }
        }.shareIn(repositoryScope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private fun observeServiceEvents() {
        repositoryScope.launch {
            musicService?.serviceEvents?.collect { event ->
                when (event) {
                    is MusicPlayerService.MusicServiceEvent.AudioOutputChanged -> {
                        Toast.makeText(
                            context,
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i("ViewModel", "Audio Output Changed: ${event.message}")
                    }

                    is MusicPlayerService.MusicServiceEvent.PlaybackPausedDueToNoise -> {
                        Toast.makeText(
                            context,
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i("ViewModel", "Playback Paused: ${event.message}")
                    }
                }
            }
        }
    }

    override suspend fun setPreferredAudioDevice(device: AudioDevice?) {
        Log.d("MusicPlayerRepo", "Setting preferred audio device: ${device?.name ?: "None"}")
        userPreferencesRepository.savePreferredAudioDevice(device)
        if (serviceBoundFlow.value) {
            musicService?.updatePreferredAudioDevice(device)
        } else {
            Log.w("MusicPlayerRepo", "Service not bound when setting preferred audio device. Will be picked up on next bind/start or by service itself.")
        }
    }

    override fun getPreferredAudioDevice(): Flow<AudioDevice?> {
        return userPreferencesRepository.preferredAudioDeviceFlow
    }

    override fun getCurrentPlayingSongId(): Flow<Long?> = serviceBoundFlow.flatMapLatest { isBound ->
        if (isBound && musicService != null) {
            musicService!!.currentSong.map { it?.id }
        } else {
            flowOf(null)
        }
    }.distinctUntilChanged()

    override fun getLiveElapsedTimeMs(): Flow<Long> = serviceBoundFlow.flatMapLatest { isBound ->
        if (isBound && musicService != null) {
            musicService!!.liveElapsedTimeMs
        } else {
            flowOf(0L)
        }
    }.distinctUntilChanged()

    override fun getCurrentSongMonthYear(): Flow<String?> = serviceBoundFlow.flatMapLatest { isBound ->
        if (isBound && musicService != null) {
            musicService!!.currentSong.map { song ->
                song?.lastPlayed?.let { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                }
            }
        } else {
            flowOf(null)
        }
    }.distinctUntilChanged()

    private suspend fun <T> callServiceMethod(
        actionDescription: String,
        block: suspend MusicPlayerService.() -> T?
    ): T? {
        if (serviceBoundFlow.value && musicService != null) {
            try {
                return musicService!!.block()
            } catch (e: Exception) {
                Log.e("MusicPlayerRepo", "Error calling service method for $actionDescription: ${e.message}", e)
                return null
            }
        } else {
            Log.w("MusicPlayerRepo", "$actionDescription: Service not bound.")
            return null
        }
    }

    override suspend fun playSong(song: Song, queue: List<Song>) {
        callServiceMethod("PlaySong") { playSong(song, queue) }
    }

    override suspend fun play() {
        callServiceMethod("Play/Pause") { playPause() }
    }

    override suspend fun pause() {
        callServiceMethod("Play/Pause") { playPause() }
    }

    override suspend fun playNext() {
        callServiceMethod("PlayNext") { playNext() }
    }

    override suspend fun playPrevious() {
        callServiceMethod("PlayPrevious") { playPrevious() }
    }

    override suspend fun seekTo(position: Long) {
        callServiceMethod("SeekTo") { seekTo(position) }
    }

    override suspend fun stopPlayback() {
        callServiceMethod("StopPlayback") { stopPlaybackAndNotification() }
    }
}