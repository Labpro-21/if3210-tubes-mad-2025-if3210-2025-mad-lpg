package com.tubes1.purritify.features.library.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.features.library.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getNewlyAddedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorited = 1 ORDER BY title ASC")
    fun getAllFavoritedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayed IS NOT NULL ORDER BY title ASC")
    fun getAllListenedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: Long): Int

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("UPDATE songs SET lastPlayed = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayed(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE songs 
        SET isFavorited = CASE 
            WHEN isFavorited = 1 THEN 0 
            ELSE 1 
        END 
        WHERE id = :songId
    """)
    suspend fun toggleFavorite(songId: Long): Int
}