package com.tubes1.purritify.features.soundcapsule.domain.model

data class MonthlyAnalytics(
    val monthYear: String,
    val totalTimeListenedMs: Long,
    val topArtists: List<TopArtist>,
    val topSongs: List<TopSong>,
    val dayStreaks: List<DayStreak>,
    val hasData: Boolean = true
) {
    companion object {
        fun noData(monthYear: String): MonthlyAnalytics = MonthlyAnalytics(
            monthYear = monthYear,
            totalTimeListenedMs = 0L,
            topArtists = emptyList(),
            topSongs = emptyList(),
            dayStreaks = emptyList(),
            hasData = false
        )
    }
}