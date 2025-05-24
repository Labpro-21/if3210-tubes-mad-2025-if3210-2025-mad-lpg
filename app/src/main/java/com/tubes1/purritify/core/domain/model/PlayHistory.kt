package com.tubes1.purritify.core.domain.model

data class PlayHistory(
    val id: Long? = null,
    val datetime: Long,
    val songId: Long,
    val artist: String,
    val month: String,
    val duration: Long
)