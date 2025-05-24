package com.tubes1.purritify.features.onlinesongs.data.repository

import com.tubes1.purritify.features.onlinesongs.data.remote.OnlineSongsApi
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.onlinesongs.domain.repository.OnlineSongsRepository

class OnlineSongsRepositoryImpl (
    private val api: OnlineSongsApi
) : OnlineSongsRepository {

    override suspend fun getTopGlobalSongs(): List<OnlineSongsDto> {
        return api.getTopGlobalSongs()
    }

    override suspend fun getTopCountrySongs(countryCode: String): List<OnlineSongsDto> {
        return api.getTopCountrySongs(countryCode)
    }

}