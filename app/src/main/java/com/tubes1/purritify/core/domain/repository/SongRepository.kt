package com.tubes1.purritify.core.domain.repository

import com.tubes1.purritify.core.data.local.entity.SongEntity
import com.tubes1.purritify.core.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {

    fun getAllSongs(): Flow<List<Song>>

    fun getNewlyAddedSongs(limit: Int): Flow<List<Song>>

    fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>>

    fun getAllFavoritedSongs(): Flow<List<Song>>

    fun getAllListenedSongs(): Flow<List<Song>>

    fun getRecommendedSongs(): Flow<List<Song>>

    suspend fun addSong(song: Song): Long

    suspend fun deleteSong(songId: Long): Boolean

    suspend fun getSongById(songId: Long): Song?

    suspend fun updateLastPlayed(songId: Long): Unit

    suspend fun toggleFavorite(songId: Long): Boolean

    suspend fun getSongByTitleAndArtistAndDuration(title: String, artist: String, duration: Long): Song?
}