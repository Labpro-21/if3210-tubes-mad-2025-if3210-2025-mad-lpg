package com.tubes1.purritify.core.domain.repository

import com.tubes1.purritify.core.domain.model.ServerSong

interface ServerSongRepository {

    suspend fun getServerSongByServerId(serverId: Long): ServerSong?


    suspend fun getServerSongByLocalId(localId: Long): ServerSong?


    suspend fun linkServerSongToLocalSong(serverId: Long, localSongId: Long, isInitiallyDownloaded: Boolean = false): ServerSong?


    suspend fun updateServerSong(serverSong: ServerSong): Int




}