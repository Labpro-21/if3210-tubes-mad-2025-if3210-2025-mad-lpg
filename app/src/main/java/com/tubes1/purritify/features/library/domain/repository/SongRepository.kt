package com.tubes1.purritify.features.library.domain.repository

import com.tubes1.purritify.features.library.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {

    fun getAllSongs(): Flow<List<Song>>

    fun getNewlyAddedSongs(limit: Int): Flow<List<Song>>

    fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>>

    fun getAllFavoritedSongs(): Flow<List<Song>>

    suspend fun addSong(song: Song): Long

    suspend fun deleteSong(songId: Long): Boolean

    suspend fun getSongById(songId: Long): Song?

    suspend fun updateLastPlayed(songId: Long)
}