package com.tubes1.purritify.features.profile.presentation.profile;

import com.tubes1.purritify.features.profile.domain.model.Profile

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val tokenExpired: Boolean = false,
    val error: String = ""
)
