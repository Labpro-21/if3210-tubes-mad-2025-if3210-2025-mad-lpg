package com.tubes1.purritify.features.onlinesongs.data.remote

import com.tubes1.purritify.features.onlinesongs.data.remote.dto.OnlineSongsDto
import com.tubes1.purritify.features.profile.data.remote.dto.ProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

interface OnlineSongsApi {
    @GET("/api/top-songs/global")
    suspend fun getTopGlobalSongs(): List<OnlineSongsDto>

    @GET("/api/top-songs/{country_code}")
    suspend fun getTopCountrySongs(@Path("country_code") countryCode: String): List<OnlineSongsDto>
}