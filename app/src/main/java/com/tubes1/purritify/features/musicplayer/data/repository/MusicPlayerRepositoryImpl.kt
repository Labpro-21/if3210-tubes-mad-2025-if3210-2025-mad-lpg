package com.tubes1.purritify.features.musicplayer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.tubes1.purritify.core.data.local.preferences.UserPreferencesRepository
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.audiorouting.domain.model.AudioDevice
import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService
import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class MusicPlayerRepositoryImpl(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : MusicPlayerRepository {

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


            repositoryScope.launch {
                userPreferencesRepository.preferredAudioDeviceFlow.firstOrNull()?.let { device ->
                    musicService?.updatePreferredAudioDevice(device)
                }
            }
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
            if (!success && !_serviceBoundFlow.value) { // Check if already bound by a quick onServiceConnected
                Log.e("MusicPlayerRepo", "Immediate bindService FAILED. Service might not be available.")
            }
        }
    }

    override fun getPlayerState(): Flow<MusicPlayerState> {
        return serviceBoundFlow.flatMapLatest { isBound -> // Use the public StateFlow
            if (isBound && musicService != null) {
                val currentService = musicService!!
                combine(
                    currentService.currentSong,
                    currentService.isPlaying,
                    currentService.currentPosition,
                    currentService.duration,
                    currentService.preferredAudioDevice // Also observe preferred device from service
                ) { song, isPlaying, position, duration, preferredDevice ->
                    MusicPlayerState(
                        currentSong = song,
                        queue = currentService.currentQueue,
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,


                        activeAudioDevice = preferredDevice // Example: adding active device info
                    )
                }
            } else {
                flowOf(MusicPlayerState(queue = emptyList()))
            }
        }.shareIn(repositoryScope, SharingStarted.WhileSubscribed(5000), 1)
    }

    override suspend fun setPreferredAudioDevice(device: AudioDevice?) {
        Log.d("MusicPlayerRepo", "Setting preferred audio device: ${device?.name ?: "None"}")
        userPreferencesRepository.savePreferredAudioDevice(device)
        if (serviceBoundFlow.value) { // Use the public StateFlow
            musicService?.updatePreferredAudioDevice(device)
        } else {
            Log.w("MusicPlayerRepo", "Service not bound when setting preferred audio device. Will be picked up on next bind/start or by service itself.")
        }
    }

    override fun getPreferredAudioDevice(): Flow<AudioDevice?> {
        return userPreferencesRepository.preferredAudioDeviceFlow
    }

    private suspend fun <T> callServiceMethod(
        actionDescription: String,
        block: suspend MusicPlayerService.() -> T?
    ): T? {
        if (serviceBoundFlow.value && musicService != null) { // Use the public StateFlow
            try {
                return musicService!!.block()
            } catch (e: Exception) {
                Log.e("MusicPlayerRepo", "Error calling service method for $actionDescription: ${e.message}", e)
                return null // Or rethrow / handle error appropriately
            }
        } else {
            Log.w("MusicPlayerRepo", "$actionDescription: Service not bound.")
            return null // Or throw an exception / queue the command
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