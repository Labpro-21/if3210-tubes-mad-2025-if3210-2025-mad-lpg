package com.tubes1.purritify.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity

@Dao
interface ServerSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerSong(serverSong: ServerSongEntity)

    @Query("SELECT * FROM server_song WHERE localSongId = :localSongId")
    suspend fun getServerSongByLocalId(localSongId: Long): ServerSongEntity?

    @Query("SELECT * FROM server_song WHERE serverSongId = :serverSongId")
    suspend fun getServerSongByServerId(serverSongId: Long): ServerSongEntity?
}