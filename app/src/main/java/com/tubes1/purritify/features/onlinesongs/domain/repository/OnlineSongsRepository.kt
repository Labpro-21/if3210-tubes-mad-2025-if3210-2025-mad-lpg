package com.tubes1.purritify.features.onlinesongs.domain.repository

import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.onlinesongs.domain.model.ChartSong
import kotlinx.coroutines.flow.Flow

interface OnlineSongsRepository {

    fun getTopGlobalChartSongs(): Flow<Resource<List<ChartSong>>>

    fun getTopCountryChartSongs(countryCode: String): Flow<Resource<List<ChartSong>>>

    fun getOnlineSong(songId: Long): Flow<Resource<ChartSong>>
}