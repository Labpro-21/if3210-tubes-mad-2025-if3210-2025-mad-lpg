package com.tubes1.purritify.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.ArtistsCount

@Dao
interface ArtistsCountDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(artist: ArtistsCount): Long

    @Query("UPDATE artists_count SET likedCount = likedCount + 1 WHERE name = :artistName")
    suspend fun incrementLike(artistName: String)

    @Query("UPDATE artists_count SET likedCount = likedCount - 1 WHERE name = :artistName")
    suspend fun decrementLike(artistName: String)
}