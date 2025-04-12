package com.tubes1.purritify.features.auth.data.remote.dto

import com.tubes1.purritify.features.auth.domain.model.Token

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
