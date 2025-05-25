package com.tubes1.purritify.features.onlinesongs.domain.model

import com.tubes1.purritify.core.domain.model.Song

data class ChartSong(
    val serverId: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMillis: Long,
    val country: String,
    val rank: Int,
    var isDownloaded: Boolean = false,
    var localSongId: Long? = null
)

fun ChartSong.toPlayerSong(): Song {
    return Song(
        id = this.localSongId,
        title = this.title,
        artist = this.artist,
        duration = this.durationMillis,
        path = this.streamUrl,
        songArtUri = this.artworkUrl,
        dateAdded = System.currentTimeMillis(),
        isFavorited = false,
        isFromServer = true
    )
}