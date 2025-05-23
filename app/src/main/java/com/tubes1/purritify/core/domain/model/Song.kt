package com.tubes1.purritify.core.domain.model

data class Song(
    val id: Long? = null,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val songArtUri: String?,
    val dateAdded: Long,
    val lastPlayed: Long?,
    val isFavorited: Boolean = false,
    val isFromServer: Boolean = false
)