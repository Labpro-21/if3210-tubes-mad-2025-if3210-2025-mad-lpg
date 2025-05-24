package com.tubes1.purritify.features.onlinesongs.data.remote.dto

import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs

data class OnlineSongsDto(
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val country: String,
    val rank: Int,
    val createdAt: String,
    val updatedAt: String
)

fun OnlineSongsDto.toOnlineSongs() : OnlineSongs {
    return OnlineSongs(
        id,
        title,
        artist,
        artwork,
        url,
        duration,
        rank
    )
}
