package com.tubes1.purritify.features.profile.data.remote

import com.tubes1.purritify.features.profile.data.remote.dto.ProfileDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApi {
    @GET("/api/profile")
    suspend fun getProfile(@Header("Authorization") authorization: String): ProfileDto

    @GET("/uploads/profile-picture/{profilePhotoPath}")
    suspend fun getProfilePhoto(@Path("profilePhotoPath") profilePhotoPath: String): ResponseBody

    @Multipart
    @PATCH("/api/profile")
    suspend fun editProfile(
        @Header("Authorization") authorization: String,
        @Part("location") location: String?,
        @Part profilePhoto: MultipartBody.Part?
    ): ResponseBody
}