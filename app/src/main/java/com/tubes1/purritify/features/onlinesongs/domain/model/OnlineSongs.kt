package com.tubes1.purritify.features.onlinesongs.domain.model

data class OnlineSongs(
    val id: Long,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val rank: Int
)
