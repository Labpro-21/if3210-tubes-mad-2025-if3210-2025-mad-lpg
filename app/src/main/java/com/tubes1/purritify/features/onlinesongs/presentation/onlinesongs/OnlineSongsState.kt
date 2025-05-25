package com.tubes1.purritify.features.onlinesongs.presentation.onlinesongs

import com.tubes1.purritify.core.domain.model.Song

enum class SongType {
    GLOBAL, COUNTRY
}

data class OnlineSongsState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val songType: SongType? = null
)
