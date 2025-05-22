package com.tubes1.purritify.features.musicplayer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.tubes1.purritify.core.data.model.Song
import com.tubes1.purritify.features.musicplayer.data.service.MusicPlayerService
import com.tubes1.purritify.features.musicplayer.domain.model.MusicPlayerState
import com.tubes1.purritify.features.musicplayer.domain.repository.MusicPlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PlayerRepositoryImpl(private val context: Context) : MusicPlayerRepository {
    private var musicService: MusicPlayerService? = null
    private var bound = false
    private val queue = mutableListOf<Song>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicPlayerBinder
            musicService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            bound = false
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }

    override fun getPlayerState(): Flow<MusicPlayerState> {
        return combine(
            musicService?.currentSong ?: kotlinx.coroutines.flow.flowOf(null),
            musicService?.isPlaying ?: kotlinx.coroutines.flow.flowOf(false),
            musicService?.currentPosition ?: kotlinx.coroutines.flow.flowOf(0L),
            musicService?.duration ?: kotlinx.coroutines.flow.flowOf(0L)
        ) { song, isPlaying, position, duration ->
            MusicPlayerState(
                currentSong = song,
                queue = queue,
                isPlaying = isPlaying,
                currentPosition = position,
                duration = duration
            )
        }
    }

    override suspend fun playSong(song: Song, queue: List<Song>) {
        this.queue.clear()
        this.queue.addAll(queue)
        musicService?.playSong(song, queue)
    }

    override suspend fun play() {
        musicService?.playPause()
    }

    override suspend fun pause() {
        musicService?.playPause()
    }

    override suspend fun playNext() {
        musicService?.playNext()
    }

    override suspend fun playPrevious() {
        musicService?.playPrevious()
    }

    override suspend fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    override suspend fun updatePosition(position: Long) {
        musicService?.seekTo(position)
    }

    override suspend fun stopPlayback() {
        musicService?.stopPlayback()
    }
}