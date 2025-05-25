package com.tubes1.purritify.core.data.repository

import com.tubes1.purritify.core.data.local.dao.ArtistsCountDao
import com.tubes1.purritify.core.data.local.dao.SongDao
import com.tubes1.purritify.core.data.local.entity.ArtistsCount
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.core.data.local.entity.toSongEntity
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.SongRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepositoryImpl(
    private val songDao: SongDao,
    private val artistsCountDao: ArtistsCountDao
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

    override fun getRecommendedSongs(): Flow<List<Song>> {
        return songDao.getRecommendedSongs()
            .map { entities -> entities.map { it.toSong() } }
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
        val song = songDao.getSongById(songId) ?: return false
        val isUpdated = songDao.toggleFavorite(songId) > 0

        fun splitArtists(artistField: String): List<String> {
            return artistField.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        if (isUpdated) {
            val artists = splitArtists(song.artist)

            if (song.isFavorited) {
                for (artist in artists) {
                    artistsCountDao.decrementLike(artist)
                }
            } else {
                for (artist in artists) {
                    val inserted = artistsCountDao.insert(ArtistsCount(artist, 1))
                    if (inserted == -1L) {
                        artistsCountDao.incrementLike(artist)
                    }
                }
            }
        }
        return isUpdated
    }

    override suspend fun getSongByTitleAndArtistAndDuration(title: String, artist: String, duration: Long): Song? {
        return songDao.getSongByTitleAndArtistAndDuration(title, artist, duration)?.toSong()
    }
}