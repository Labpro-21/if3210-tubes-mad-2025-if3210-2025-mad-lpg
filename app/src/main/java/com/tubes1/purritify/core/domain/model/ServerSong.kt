package com.tubes1.purritify.core.domain.model

data class ServerSong(
    val localSongId: Long,
    val serverSongId: Long,
    val isDownloaded: Boolean
)