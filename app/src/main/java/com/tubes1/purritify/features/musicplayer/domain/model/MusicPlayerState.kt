package com.tubes1.purritify.features.musicplayer.domain.model

import com.tubes1.purritify.core.data.model.Song

data class MusicPlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)