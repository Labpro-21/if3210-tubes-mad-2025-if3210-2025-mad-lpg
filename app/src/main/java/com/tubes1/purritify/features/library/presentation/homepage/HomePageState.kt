package com.tubes1.purritify.features.library.presentation.homepage

import com.tubes1.purritify.core.domain.model.Song

data class HomePageState(
    val newlyAddedSongs: List<Song> = emptyList(),
    val recentlyPlayedSongs: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val isLoadingNewSongs: Boolean = false,
    val isLoadingRecentSongs: Boolean = false,
    val isLoadingRecommendedSongs: Boolean = false,
    val error: String? = null
)