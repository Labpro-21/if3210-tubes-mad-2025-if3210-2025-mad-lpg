package com.tubes1.purritify.features.auth.domain.repository

import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.LoginResponse
import com.tubes1.purritify.features.auth.data.remote.dto.RefreshRequest

interface LoginRepository {
    suspend fun login(body: LoginRequest): LoginResponse
    suspend fun refresh(body: RefreshRequest): LoginResponse
}