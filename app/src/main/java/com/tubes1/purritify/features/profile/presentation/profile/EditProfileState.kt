package com.tubes1.purritify.features.profile.presentation.profile

data class EditProfileState (
    val isLoading: Boolean = false,
    val isEditSuccess: Boolean = false,
    val tokenExpired: Boolean = false,
    val error: String = ""
)