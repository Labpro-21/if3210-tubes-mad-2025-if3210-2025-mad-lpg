package com.tubes1.purritify.features.auth.data.repository

import com.tubes1.purritify.features.auth.data.remote.LoginApi
import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.LoginResponse
import com.tubes1.purritify.features.auth.data.remote.dto.RefreshRequest
import com.tubes1.purritify.features.auth.domain.repository.LoginRepository

class LoginRepositoryImpl (
    private val api: LoginApi
) : LoginRepository {

    override suspend fun login(body: LoginRequest): LoginResponse {
        return api.login(body)
    }

    override suspend fun refresh(body: RefreshRequest): LoginResponse {
        return api.refresh(body)
    }
}