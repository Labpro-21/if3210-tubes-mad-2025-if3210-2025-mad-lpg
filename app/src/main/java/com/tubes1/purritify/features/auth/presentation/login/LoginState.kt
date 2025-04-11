package com.tubes1.purritify.features.auth.presentation.login

import com.tubes1.purritify.features.auth.domain.model.Token

data class LoginState (
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val token: Token? = null,
    val error: String = "",
)