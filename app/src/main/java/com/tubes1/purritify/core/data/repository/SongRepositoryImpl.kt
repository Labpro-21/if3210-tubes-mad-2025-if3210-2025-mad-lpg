package com.tubes1.purritify.core.data.repository

import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.core.data.local.entity.toSongEntity
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepositoryImpl(
    private val songDao: SongDao
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getNewlyAddedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getNewlyAddedSongs(limit).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>> {
        return songDao.getRecentlyPlayedSongs(limit).map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getAllFavoritedSongs(): Flow<List<Song>> {
        return songDao.getAllFavoritedSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override fun getAllListenedSongs(): Flow<List<Song>> {
        return songDao.getAllListenedSongs().map { entities ->
            entities.map { it.toSong() }
        }
    }

    override suspend fun addSong(song: Song): Long {
        return songDao.insertSong(song.toSongEntity())
    }

    override suspend fun deleteSong(songId: Long): Boolean {
        return songDao.deleteSong(songId) > 0
    }

    override suspend fun getSongById(songId: Long): Song? {
        return songDao.getSongById(songId)?.toSong()
    }

    override suspend fun updateLastPlayed(songId: Long) {
        songDao.updateLastPlayed(songId)
    }

    override suspend fun toggleFavorite(songId: Long): Boolean {
        return songDao.toggleFavorite(songId) > 0
    }

    override suspend fun getSongByTitleAndArtistAndDuration(title: String, artist: String, duration: Long): Song? {
        return songDao.getSongByTitleAndArtistAndDuration(title, artist, duration)?.toSong()
    }
}