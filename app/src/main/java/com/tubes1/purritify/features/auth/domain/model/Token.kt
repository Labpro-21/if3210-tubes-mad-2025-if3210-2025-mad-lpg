package com.tubes1.purritify.features.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Token (
    val accessToken: String = "",
    val refreshToken: String = ""
)