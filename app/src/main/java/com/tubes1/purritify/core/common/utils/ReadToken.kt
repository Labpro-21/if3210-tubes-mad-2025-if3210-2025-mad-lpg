package com.tubes1.purritify.core.common.utils

import android.util.Log
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
            Log.d("TOKEN_STATUS", "Case 1: Akses token belum expired")
            return currentAccessToken
        }

        val currentRefreshToken = getRefreshTokenUseCase()
        if (verifyTokenUseCase(currentRefreshToken)) {
            Log.d("TOKEN_STATUS", "Case 2: Akses token expired, tapi Refresh token belum")
            refreshTokenUseCase(currentRefreshToken).collect { resource ->
                when(resource) {
                    is Resource.Success -> {
                        currentAccessToken = resource.data?.accessToken ?: ""
                    } is Resource.Loading -> {

                    } else -> {
                        Log.e("TOKEN_REFRESH_ERROR", resource.message ?: "Unknown error")
                    }
                }
            }
            return currentAccessToken
        }

        Log.d("TOKEN_STATUS", "Case 3: Dua-duanya expired")
        return ""
    }
}