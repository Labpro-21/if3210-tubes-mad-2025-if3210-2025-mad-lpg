package com.tubes1.purritify.features.onlinesongs.data.repository

import android.util.Log
import com.tubes1.purritify.core.data.local.ServerSongDao
import com.tubes1.purritify.core.data.local.SongDao
import com.tubes1.purritify.core.data.local.entity.toSong
import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.core.domain.repository.ServerSongRepository
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toSong
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OnlineSongsRepositoryImpl (
    private val api: OnlineSongsApi,
    private val serverSongRepository: ServerSongRepository,
    private val songDao: SongDao
) : OnlineSongsRepository {

    override fun getTopGlobalSongs(): Flow<List<Song>> = flow {
        try {
            val response = api.getTopGlobalSongs()
            Log.d("TAG","AAAAAA")
            val songs = response.map { song ->
                val localId = serverSongRepository.getSongLocalId(song.id)
                Log.d("TAG","PROCESSING ${localId}")
                if (localId == null) {
                    song.toSong()
                } else {
                    songDao.getSongById(localId)!!.toSong()
                }
            }
            emit(songs)
        } catch (e: Exception) {
            Log.d("ERROR", "NDIJACOJOA ${e}")
        }

    }

    override fun getTopCountrySongs(countryCode: String): Flow<List<Song>> = flow {
        val response = api.getTopCountrySongs(countryCode)
        val songs = response.map { song ->
            val localId = serverSongRepository.getSongLocalId(song.id)
            if (localId == null) {
                song.toSong()
            } else {
                songDao.getSongById(localId)!!.toSong()
            }
        }
        emit(songs)
    }


}