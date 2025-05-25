package com.tubes1.purritify.features.soundcapsule.domain.model

data class TopSong(
    val songId: Long,
    val title: String,
    val artist: String,
    val songArtUri: String?,
    val playCount: Int,
    val totalDurationMs: Long,
    val rank: Int? = null
)