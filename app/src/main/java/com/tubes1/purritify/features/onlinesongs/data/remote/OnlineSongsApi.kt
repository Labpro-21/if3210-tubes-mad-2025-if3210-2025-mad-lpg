package com.tubes1.purritify.features.onlinesongs.data.remote

import com.google.android.gms.common.api.Response
import com.tubes1.purritify.features.onlinesongs.data.remote.dto.ServerSongDto
import retrofit2.http.GET
import retrofit2.http.Path

interface OnlineSongsApi {
    companion object {
        val SUPPORTED_COUNTRY_CODES = setOf("ID", "MY", "US", "GB", "CH", "DE", "BR")
        const val COUNTRY_CODE_GLOBAL = "GLOBAL"
    }

    @GET("api/top-songs/global")
    suspend fun getTopGlobalSongs(): List<ServerSongDto> 

    @GET("api/top-songs/{country_code}")
    suspend fun getTopCountrySongs(
        @Path("country_code") countryCode: String
    ): List<ServerSongDto>
}