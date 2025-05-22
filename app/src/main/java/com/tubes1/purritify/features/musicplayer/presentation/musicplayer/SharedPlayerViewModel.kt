package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import androidx.lifecycle.ViewModel
import com.tubes1.purritify.core.data.model.Song
import kotlinx.coroutines.flow.StateFlow

class SharedPlayerViewModel : ViewModel() {
    val selectedSong: StateFlow<Song?> = SharedPlayerState.currentPlayingSong
    val queue: StateFlow<List<Song>> = SharedPlayerState.musicQueue

    fun setSongAndQueue(song: Song, songQueue: List<Song>) {
        SharedPlayerState.updateSongAndQueue(song, songQueue)
    }

    fun updateSelectedSong(song: Song?) {
        SharedPlayerState.updateCurrentSong(song)
    }

    fun updateQueue(songQueue: List<Song>) {
        SharedPlayerState.updateQueue(songQueue)
    }
}
