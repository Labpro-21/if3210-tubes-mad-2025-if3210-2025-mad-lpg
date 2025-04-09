package com.tubes1.purritify.features.library.data.repository

import com.tubes1.purritify.features.library.data.local.SongDao
import com.tubes1.purritify.features.library.data.local.entity.toSong
import com.tubes1.purritify.features.library.data.local.entity.toSongEntity
import com.tubes1.purritify.features.library.domain.model.Song
import com.tubes1.purritify.features.library.domain.repository.SongRepository

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
}