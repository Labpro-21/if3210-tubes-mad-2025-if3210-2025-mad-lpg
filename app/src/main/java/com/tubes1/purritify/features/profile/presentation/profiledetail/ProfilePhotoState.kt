package com.tubes1.purritify.features.profile.presentation.profiledetail

import okhttp3.ResponseBody

data class ProfilePhotoState(
    val isLoading: Boolean = false,
    val profilePhoto: ResponseBody? = null,
    val tokenExpired: Boolean = false,
    val error: String = ""
)