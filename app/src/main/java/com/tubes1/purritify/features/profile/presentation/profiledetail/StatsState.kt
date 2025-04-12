package com.tubes1.purritify.features.profile.presentation.profiledetail

import com.tubes1.purritify.features.profile.domain.model.Stats

data class StatsState(
    val isLoading: Boolean = false,
    val stats: Stats? = null,
    val tokenExpired: Boolean = false,
    val error: String = ""
)
