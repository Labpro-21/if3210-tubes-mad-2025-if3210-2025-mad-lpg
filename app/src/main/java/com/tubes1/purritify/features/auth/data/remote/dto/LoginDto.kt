package com.tubes1.purritify.features.auth.data.remote.dto

import com.tubes1.purritify.features.auth.domain.model.Token

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

fun LoginResponse.toToken(): Token {
    return Token(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}
