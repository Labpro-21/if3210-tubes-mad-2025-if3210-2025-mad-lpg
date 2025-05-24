package com.tubes1.purritify.core.common.utils

import com.tubes1.purritify.features.auth.domain.usecase.token.GetAccessTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.token.GetRefreshTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.token.RefreshTokenUseCase
import com.tubes1.purritify.features.auth.domain.usecase.token.VerifyTokenUseCase

class ReadToken(
    private val getAccessTokenUseCase: GetAccessTokenUseCase,
    private val getRefreshTokenUseCase: GetRefreshTokenUseCase,
    private val verifyTokenUseCase: VerifyTokenUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) {
    suspend operator fun invoke(): String {
        var currentAccessToken = getAccessTokenUseCase()
        if (verifyTokenUseCase(currentAccessToken)) {
            return currentAccessToken
        }

        val currentRefreshToken = getRefreshTokenUseCase()
        if (verifyTokenUseCase(currentRefreshToken)) {
            refreshTokenUseCase(currentRefreshToken).collect { resource ->
                when(resource) {
                    is Resource.Success -> {
                        currentAccessToken = resource.data?.accessToken ?: ""
                    } is Resource.Loading -> {

                    } else -> {

                    }
                }
            }
            return currentAccessToken
        }

        return ""
    }
}