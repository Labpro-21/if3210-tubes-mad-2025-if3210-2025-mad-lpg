package com.tubes1.purritify.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity
import com.tubes1.purritify.core.domain.model.ServerSong

@Dao
interface ServerSongDao {
    @Query("SELECT localSongId FROM server_song WHERE serverSongId = :serverId")
    suspend fun findSongLocalId(serverId: Long): Long?

    @Query("SELECT serverSongId FROM server_song WHERE localSongId = :localId")
    suspend fun findSongServerId(localId: Long): Long?

    @Query("SELECT isDownloaded FROM server_song WHERE serverSongId = :serverId")
    suspend fun isDownloadedServer(serverId: Long): Boolean?

    @Query("SELECT isDownloaded FROM server_song WHERE serverSongId = :localId")
    suspend fun isDownloadedLocal(localId: Long): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerSong(serverSong: ServerSongEntity): Long
}