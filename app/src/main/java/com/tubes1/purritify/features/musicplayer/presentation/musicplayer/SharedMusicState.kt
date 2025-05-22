package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import com.tubes1.purritify.core.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SharedPlayerState {
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong = _currentPlayingSong.asStateFlow()

    private val _musicQueue = MutableStateFlow<List<Song>>(emptyList())
    val musicQueue = _musicQueue.asStateFlow()

    fun updateSongAndQueue(song: Song?, queue: List<Song>) {
        _currentPlayingSong.value = song
        _musicQueue.value = queue
    }

    fun updateCurrentSong(song: Song?) {
        _currentPlayingSong.value = song
    }

    fun updateQueue(queue: List<Song>) {
        _musicQueue.value = queue
    }
}