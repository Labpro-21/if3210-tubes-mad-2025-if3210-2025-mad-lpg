package com.tubes1.purritify.features.auth.data.repository

import com.tubes1.purritify.features.auth.data.remote.AuthApi
import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.LoginResponse
import com.tubes1.purritify.features.auth.data.remote.dto.RefreshRequest
import com.tubes1.purritify.features.auth.data.remote.dto.VerifyTokenResponse
import com.tubes1.purritify.features.auth.domain.repository.AuthRepository

class AuthRepositoryImpl (
    private val api: AuthApi
) : AuthRepository {
    override suspend fun login(body: LoginRequest): LoginResponse {
        return api.login(body)
    }

    override suspend fun refresh(body: RefreshRequest): LoginResponse {
        return api.refresh(body)
    }

    override suspend fun verify(token: String): VerifyTokenResponse {
        return api.verify("Bearer $token")
    }
}