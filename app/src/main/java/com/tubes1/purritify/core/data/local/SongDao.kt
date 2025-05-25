package com.tubes1.purritify.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM song ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song ORDER BY dateAdded DESC LIMIT :limit")
    fun getNewlyAddedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE isFavorited = 1 ORDER BY title ASC")
    fun getAllFavoritedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE lastPlayed IS NOT NULL ORDER BY title ASC")
    fun getAllListenedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("DELETE FROM song WHERE id = :songId")
    suspend fun deleteSong(songId: Long): Int

    @Query("SELECT * FROM song WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("UPDATE song SET lastPlayed = :timestamp WHERE id = :songId")
    suspend fun updateLastPlayed(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE song 
        SET isFavorited = CASE 
            WHEN isFavorited = 1 THEN 0 
            ELSE 1 
        END 
        WHERE id = :songId
    """)
    suspend fun toggleFavorite(songId: Long): Int

    @Query("SELECT * FROM song WHERE title = :title AND artist = :artist AND duration = :duration")
    suspend fun getSongByTitleAndArtistAndDuration(title: String, artist: String, duration: Long): SongEntity?
}