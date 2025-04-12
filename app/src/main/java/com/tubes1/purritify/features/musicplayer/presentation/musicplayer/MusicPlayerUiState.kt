package com.tubes1.purritify.features.musicplayer.presentation.musicplayer

import com.tubes1.purritify.features.library.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)
