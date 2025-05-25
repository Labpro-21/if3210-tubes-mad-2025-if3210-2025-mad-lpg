package com.tubes1.purritify.core.data.repository

import com.tubes1.purritify.core.data.local.ServerSongDao
import com.tubes1.purritify.core.data.local.SongDao
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity
import com.tubes1.purritify.core.data.local.entity.toSongEntity
import com.tubes1.purritify.core.domain.model.ServerSong
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository

class ServerSongRepositoryImpl (
    private val songDao: SongDao,
    private val serverSongDao: ServerSongDao
): ServerSongRepository {

    override suspend fun getSongServerId(localId: Long): Long? {
        return serverSongDao.findSongServerId(localId)
    }

    override suspend fun getSongLocalId(serverId: Long): Long? {
        return serverSongDao.findSongLocalId(serverId)
    }

    override suspend fun getIsDownloadedServer(serverId: Long): Boolean? {
        return serverSongDao.isDownloadedServer(serverId)
    }

    override suspend fun getIsDownloadedLocal(localId: Long): Boolean? {
        return serverSongDao.isDownloadedLocal(localId)
    }

    override suspend fun insertSong(song: Song): Long? {
        val localId = songDao.insertSong(song.toSongEntity())
        val serverSong = ServerSongEntity(
            serverSongId = song.id!!,
            localSongId = localId,
            isDownloaded = false
        )
        return serverSongDao.insertServerSong(serverSong)
    }

}