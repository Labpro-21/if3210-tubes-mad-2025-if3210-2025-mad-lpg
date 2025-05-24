package com.tubes1.purritify.features.musicplayer.data.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.tubes1.purritify.core.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException


class MusicPlayerService : Service() {
    private val binder = MusicPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentQueue: List<Song> = emptyList()
    private var currentSongIndex: Int = -1
    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { playNext() }
            setOnPreparedListener {
                _duration.value = mediaPlayer?.duration?.toLong() ?: 0L
                start()
                _isPlaying.value = true
                startPositionUpdates()
            }
            setOnErrorListener { _, _, _ ->
                stopPositionUpdates()
                _isPlaying.value = false
                true
            }
        }
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        super.onDestroy()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        stopPlayback()

        currentQueue = queue
        currentSongIndex = queue.indexOf(song).takeIf { it >= 0 } ?: 0
        _currentSong.value = song

        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { playNext() }
            setOnPreparedListener {
                _duration.value = mediaPlayer?.duration?.toLong() ?: 0L
                start()
                _isPlaying.value = true
                startPositionUpdates()
            }
            setOnErrorListener { _, _, _ ->
                stopPositionUpdates()
                _isPlaying.value = false
                true
            }

            try {
                setDataSource(song.path)
                prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun playPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopPositionUpdates()
            } else {
                it.start()
                _isPlaying.value = true
                startPositionUpdates()
            }
        }
    }

    fun playNext() {
        if (currentQueue.isEmpty() || currentSongIndex == -1) return

        currentSongIndex = (currentSongIndex + 1) % currentQueue.size
        playSong(currentQueue[currentSongIndex], currentQueue)
    }

    fun playPrevious() {
        if (currentQueue.isEmpty() || currentSongIndex == -1) return

        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            seekTo(0)
            return
        }

        currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else currentQueue.size - 1
        playSong(currentQueue[currentSongIndex], currentQueue)
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    private fun startPositionUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (true) {
                mediaPlayer?.let {
                    _currentPosition.value = it.currentPosition.toLong()
                }
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
        }

        stopPositionUpdates()

        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _currentSong.value = null
        currentSongIndex = -1
        currentQueue = emptyList()
    }


    private fun releaseMediaPlayer() {
        stopPositionUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }
}