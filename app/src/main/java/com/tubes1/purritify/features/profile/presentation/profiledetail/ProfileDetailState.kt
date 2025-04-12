package com.tubes1.purritify.features.profile.presentation.profiledetail;

import com.tubes1.purritify.features.profile.domain.model.Profile

data class ProfileDetailState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val tokenExpired: Boolean = false,
    val error: String = ""
)
