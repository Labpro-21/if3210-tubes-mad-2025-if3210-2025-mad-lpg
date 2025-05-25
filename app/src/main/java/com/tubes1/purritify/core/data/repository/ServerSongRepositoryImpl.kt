package com.tubes1.purritify.core.data.repository

import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.dao.ServerSongDao
import com.tubes1.purritify.core.data.local.entity.ServerSongEntity
import com.tubes1.purritify.core.data.local.entity.toServerSong
import com.tubes1.purritify.core.data.local.entity.toServerSongEntity
import com.tubes1.purritify.core.data.local.entity.toSongEntity
import com.tubes1.purritify.core.domain.model.ServerSong
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository

class ServerSongRepositoryImpl(
    private val serverSongDao: ServerSongDao
) : ServerSongRepository {

    override suspend fun getServerSongByServerId(serverId: Long): ServerSong? {
        return serverSongDao.getServerSongByServerId(serverId)?.toServerSong()
    }

    override suspend fun getServerSongByLocalId(localId: Long): ServerSong? {
        return serverSongDao.getServerSongByLocalId(localId)?.toServerSong()
    }

    override suspend fun linkServerSongToLocalSong(
        serverId: Long,
        localSongId: Long,
        isInitiallyDownloaded: Boolean
    ): ServerSong? {
        val newServerSongEntity = ServerSongEntity(
            serverSongId = serverId,
            localSongId = localSongId,
            isDownloaded = isInitiallyDownloaded
        )
        serverSongDao.insertServerSong(newServerSongEntity)

        return newServerSongEntity.toServerSong()
    }

    override suspend fun updateServerSong(serverSong: ServerSong): Int {
        return serverSongDao.updateServerSong(serverSong.toServerSongEntity())
    }
}
