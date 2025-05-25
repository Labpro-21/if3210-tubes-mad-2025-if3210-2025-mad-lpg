package com.tubes1.purritify.features.soundcapsule.domain.model

data class TopArtist(
    val name: String,
    val playCount: Int,
    val totalDurationMs: Long,
    val rank: Int? = null
)