package com.tubes1.purritify.features.onlinesongs.domain.repository

import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto

interface OnlineSongsRepository {
    suspend fun getTopGlobalSongs(): List<OnlineSongsDto>
    suspend fun getTopCountrySongs(countryCode: String): List<OnlineSongsDto>
}