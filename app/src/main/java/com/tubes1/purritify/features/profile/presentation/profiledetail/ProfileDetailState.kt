package com.tubes1.purritify.features.profile.presentation.profiledetail;

import com.tubes1.purritify.features.profile.domain.model.Profile

data class CoinDetailState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val error: String = ""
)
