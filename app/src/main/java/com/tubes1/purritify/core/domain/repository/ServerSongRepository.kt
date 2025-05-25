package com.tubes1.purritify.core.domain.repository

import com.tubes1.purritify.core.domain.model.Song

interface ServerSongRepository {
    suspend fun getSongLocalId(serverId: Long): Long?
    suspend fun getSongServerId(localId: Long): Long?
    suspend fun getIsDownloadedServer(serverId: Long): Boolean?
    suspend fun getIsDownloadedLocal(localId: Long): Boolean?
    suspend fun insertSong(song: Song): Long?
}