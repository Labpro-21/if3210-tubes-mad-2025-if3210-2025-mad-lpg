package com.tubes1.purritify.features.auth.domain.usecase.auth

import android.util.Log

class ReadTokenUseCase(
    private val getAccessTokenUseCase: GetAccessTokenUseCase,
    private val getRefreshTokenUseCase: GetRefreshTokenUseCase,
    private val verifyTokenUseCase: VerifyTokenUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
) {
    suspend fun invoke(): String {
        val currentAccessToken = getAccessTokenUseCase()
        if (verifyTokenUseCase(currentAccessToken)) {
            Log.d("TOKEN_STATUS", "Case 1: Akses token belum expired")
            return currentAccessToken
        }

        val currentRefreshToken = getRefreshTokenUseCase()
        if (verifyTokenUseCase(currentRefreshToken)) {
            Log.d("TOKEN_STATUS", "Case 2: Akses token expired, tapi Refresh token belum")
            refreshTokenUseCase(currentRefreshToken)
            return getAccessTokenUseCase()
        }

        Log.d("TOKEN_STATUS", "Case 3: Dua-duanya expired")
        return ""
    }
}