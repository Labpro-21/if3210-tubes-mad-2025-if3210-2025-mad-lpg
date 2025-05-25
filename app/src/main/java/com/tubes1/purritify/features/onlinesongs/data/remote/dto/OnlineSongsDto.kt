package com.tubes1.purritify.features.onlinesongs.data.remote.dto

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.onlinesongs.domain.model.OnlineSongs
import java.time.Instant

data class OnlineSongsDto(
    val id: Long,
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

fun OnlineSongsDto.toSong() : Song {
    return Song(
        id,
        title,
        artist,
        durationToMillis(duration),
        url,
        artwork,
        isoToMillis(createdAt),
        isFromServer = true
    )
}

private fun durationToMillis(duration: String): Long {
    val parts = duration.split(":")
    val minutes = parts.getOrNull(0)?.toLongOrNull() ?: 0
    val seconds = parts.getOrNull(1)?.toLongOrNull() ?: 0
    return (minutes * 60 + seconds) * 1000
}

private fun isoToMillis(isoDate: String): Long {
    return Instant.parse(isoDate).toEpochMilli()
}
