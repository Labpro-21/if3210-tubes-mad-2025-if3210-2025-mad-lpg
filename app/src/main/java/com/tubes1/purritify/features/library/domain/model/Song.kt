package com.tubes1.purritify.features.library.domain.model

data class Song(
    val id: Long? = null,
    val title: String,
    val artist: String,
    val duration: Long, // milliseconds
    val path: String,
    val songArtUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val isFavorited: Boolean = false
)