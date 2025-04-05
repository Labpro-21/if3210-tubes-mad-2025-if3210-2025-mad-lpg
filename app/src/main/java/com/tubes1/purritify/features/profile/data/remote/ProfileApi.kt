package com.tubes1.purritify.features.profile.data.remote

import com.tubes1.purritify.features.profile.data.remote.dto.ProfileDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ProfileApi {
    @GET("/api/profile")
    suspend fun getProfile(@Header("Authorization") authorization: String): ProfileDto

    @GET("/uploads/profile-picture/{profilePhotoPath}")
    suspend fun getProfilePhoto(@Path("profilePhotoPath") profilePhotoPath: String): ResponseBody
}