package com.tubes1.purritify.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

data class ArtistPlayCount(
    val artist: String,
    val playCount: Int, 
    val totalDuration: Long 
)

data class SongPlayCount(
    val songId: Long,
    val title: String, 
    val artist: String,
    val playCount: Int,
    val totalDuration: Long
)

@Dao
interface PlayHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) 
    suspend fun insertPlayHistory(playHistory: PlayHistoryEntity): Long

    @Query("SELECT SUM(duration) FROM play_history WHERE month = :monthYear")
    fun getTotalTimeListenedForMonth(monthYear: String): Flow<Long?> 

    @Query("""
        SELECT artist, COUNT(DISTINCT songId) as playCount, SUM(duration) as totalDuration 
        FROM play_history 
        WHERE month = :monthYear 
        GROUP BY artist 
        ORDER BY playCount DESC, totalDuration DESC
        LIMIT :limit
    """)
    fun getTopArtistsForMonth(monthYear: String, limit: Int): Flow<List<ArtistPlayCount>>

    @Query("""
        SELECT ph.songId, s.title, s.artist, COUNT(ph.songId) as playCount, SUM(ph.duration) as totalDuration
        FROM play_history ph
        INNER JOIN song s ON ph.songId = s.id
        WHERE ph.month = :monthYear
        GROUP BY ph.songId, s.title, s.artist
        ORDER BY playCount DESC, totalDuration DESC
        LIMIT :limit
    """)
    fun getTopSongsForMonth(monthYear: String, limit: Int): Flow<List<SongPlayCount>>

    @Query("SELECT * FROM play_history WHERE songId = :songId ORDER BY datetime ASC")
    fun getPlayHistoryForSong(songId: Long): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE month = :monthYear ORDER BY datetime DESC")
    fun getAllPlayHistoryForMonth(monthYear: String): Flow<List<PlayHistoryEntity>>

    @Query("SELECT DISTINCT songId FROM play_history WHERE month = :monthYear")
    fun getDistinctPlayedSongIdsForMonth(monthYear: String): Flow<List<Long>>

    @Query("SELECT EXISTS (SELECT 1 FROM play_history WHERE songId = :songId AND datetime >= :dayStartMillis AND datetime < :dayEndMillis LIMIT 1)")
    suspend fun wasSongPlayedOnDay(songId: Long, dayStartMillis: Long, dayEndMillis: Long): Boolean
}