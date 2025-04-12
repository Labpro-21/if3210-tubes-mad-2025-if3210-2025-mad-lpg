package com.tubes1.purritify.features.auth.domain.usecase.auth

import com.tubes1.purritify.features.auth.domain.repository.AuthRepository

class VerifyTokenUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(token: String): Boolean {
        return try {
            val response = repository.verify(token)
            response.valid
        } catch (e: Exception) {
            false
        }
    }
}