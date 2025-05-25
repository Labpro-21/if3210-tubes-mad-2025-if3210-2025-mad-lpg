package com.tubes1.purritify.features.soundcapsule.domain.model

data class DayStreak(
    val songId: Long,
    val title: String,
    val artist: String,
    val songArtUri: String?,
    val streakDays: Int,
    val lastDayOfStreak: Long
)