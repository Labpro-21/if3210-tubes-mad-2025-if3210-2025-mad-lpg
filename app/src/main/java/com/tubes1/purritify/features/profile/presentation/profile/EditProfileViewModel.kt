package com.tubes1.purritify.features.profile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.ReadToken
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.profile.domain.usecase.editprofile.EditProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class EditProfileViewModel (
    val editProfileUseCase: EditProfileUseCase,
    val readToken: ReadToken
): ViewModel() {
    private val _state = MutableStateFlow(EditProfileState())
    val state:StateFlow<EditProfileState> = _state.asStateFlow()

    fun resetState() {
        _state.value = _state.value.copy(
            isEditSuccess = false,
            error = ""
        )
    }

    fun sendNewProfile(location: String? = null, profilePhoto: MultipartBody.Part? = null) {
        viewModelScope.launch {
            val token = readToken()
            if (token == "") {
                _state.value = _state.value.copy(
                    tokenExpired = true
                )
                return@launch
            }

            try {
                editProfileUseCase(token, location, profilePhoto)
                    .collect { resource ->
                        when(resource) {
                            is Resource.Success -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    isEditSuccess = true,
                                    error = ""
                                )
                            }
                            is Resource.Error -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = resource.message ?: "Gagal mengedit profil"
                                )
                            }
                            is Resource.Loading -> {
                                _state.value = _state.value.copy(
                                    isLoading = true
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat edit profil"
                )
            }
        }
    }
}