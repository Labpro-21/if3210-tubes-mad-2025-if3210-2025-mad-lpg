package com.tubes1.purritify.features.onlinesongs.presentation

import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong

data class OnlineChartsState(
    val chartTitle: String = "",
    val chartSongs: List<ChartSong> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentCountryCode: String? = null
)