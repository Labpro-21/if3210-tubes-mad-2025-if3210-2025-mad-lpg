package com.tubes1.purritify.features.onlinesongs.data.repository

import android.util.Log
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.data.local.dao.ServerSongDao
import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.ServerSongDto
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.toChartSong
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

class OnlineSongsRepositoryImpl(
    private val onlineSongsApi: OnlineSongsApi,
    private val serverSongDao: ServerSongDao
) : OnlineSongsRepository {

    private companion object {
        const val TAG = "OnlineSongsRepo"
    }

    override fun getTopGlobalChartSongs(): Flow<Resource<List<ChartSong>>> = flow {
        emit(Resource.Loading())
        try {
            val serverSongsDto = onlineSongsApi.getTopGlobalSongs() 
            val chartSongs = mapServerSongsToChartSongs(serverSongsDto)
            emit(Resource.Success(chartSongs))
        } catch (e: HttpException) { 
            val errorMsg = e.response()?.errorBody()?.string() ?: "Failed to fetch global songs (Code: ${e.code()})"
            Log.e(TAG, "getTopGlobalChartSongs HTTP error: $errorMsg", e)
            emit(Resource.Error(errorMsg))
        } catch (e: IOException) { 
            Log.e(TAG, "getTopGlobalChartSongs IO error: ${e.message}", e)
            emit(Resource.Error("Network error fetching global songs. Please check your connection."))
        } catch (e: Exception) { 
            Log.e(TAG, "getTopGlobalChartSongs general error: ${e.message}", e)
            emit(Resource.Error("An unexpected error occurred: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getTopCountryChartSongs(countryCode: String): Flow<Resource<List<ChartSong>>> = flow {
        if (!OnlineSongsApi.SUPPORTED_COUNTRY_CODES.contains(countryCode.uppercase())) {
            emit(Resource.Error("Charts for country $countryCode are not supported by the server."))
            return@flow
        }

        emit(Resource.Loading())
        try {
            val serverSongsDto = onlineSongsApi.getTopCountrySongs(countryCode) 
            val chartSongs = mapServerSongsToChartSongs(serverSongsDto)
            emit(Resource.Success(chartSongs))
        } catch (e: HttpException) { 
            val errorMsg = e.response()?.errorBody()?.string() ?: "Failed to fetch songs for $countryCode (Code: ${e.code()})"
            Log.e(TAG, "getTopCountryChartSongs HTTP error for $countryCode: $errorMsg", e)
            emit(Resource.Error(errorMsg))
        } catch (e: IOException) { 
            Log.e(TAG, "getTopCountryChartSongs IO error for $countryCode: ${e.message}", e)
            emit(Resource.Error("Network error fetching songs for $countryCode. Please check your connection."))
        } catch (e: Exception) { 
            Log.e(TAG, "getTopCountryChartSongs general error for $countryCode: ${e.message}", e)
            emit(Resource.Error("An unexpected error occurred for $countryCode: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun mapServerSongsToChartSongs(dtos: List<ServerSongDto>): List<ChartSong> {
        return dtos.map { dto ->
            val serverSongEntity = serverSongDao.getServerSongByServerId(dto.id)
            dto.toChartSong(
                isDownloaded = serverSongEntity?.isDownloaded ?: false,
                localSongId = serverSongEntity?.localSongId
            )
        }.sortedBy { it.rank }
    }
}