package com.tubes1.purritify.features.onlinesongs.domain.repository

import com.tubes1.purritify.core.domain.model.Song
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import kotlinx.coroutines.flow.Flow

interface OnlineSongsRepository {
    fun getTopGlobalSongs(): Flow<List<Song>>
    fun getTopCountrySongs(countryCode: String): Flow<List<Song>>
}