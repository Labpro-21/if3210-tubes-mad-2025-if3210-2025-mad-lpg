package com.tubes1.purritify.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists_count")
data class ArtistsCount(
    @PrimaryKey
    val name: String,
    val likedCount: Int
)