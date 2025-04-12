package com.tubes1.purritify.features.auth.data.remote

import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.LoginResponse
import com.tubes1.purritify.features.auth.data.remote.dto.RefreshRequest
import com.tubes1.purritify.features.auth.data.remote.dto.VerifyTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


interface AuthApi {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/refresh-token")
    suspend fun refresh(@Body request: RefreshRequest): LoginResponse

    @GET("api/verify-token")
    suspend fun verify(@Header("Authorization") token: String): VerifyTokenResponse
}

