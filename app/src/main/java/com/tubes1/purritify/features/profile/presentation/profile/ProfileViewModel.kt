package com.tubes1.purritify.features.profile.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.DeleteToken
import com.tubes1.purritify.core.common.utils.ReadToken
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.domain.usecase.getsongs.GetAllFavoritedSongsUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getsongs.GetAllListenedSongsUseCase
import com.tubes1.purritify.core.domain.usecase.getsongs.GetAllSongsUseCase
import com.tubes1.purritify.features.profile.domain.model.Stats
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfilePhotoUseCase
import com.tubes1.purritify.features.profile.domain.usecase.getprofile.GetProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val readToken: ReadToken,
    private val deleteToken: DeleteToken,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePhotoUseCase: GetProfilePhotoUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val getAllFavoritedSongsUseCase: GetAllFavoritedSongsUseCase,
    private val getAllListenedSongsUseCase: GetAllListenedSongsUseCase
): ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _profilePhoto = MutableStateFlow(ProfilePhotoState())
    val profilePhoto: StateFlow<ProfilePhotoState> = _profilePhoto.asStateFlow()

    private val _userStats = MutableStateFlow(StatsState())
    val userStats: StateFlow<StatsState> = _userStats.asStateFlow()

    fun onScreenOpened() {
        getProfile()
        getStats()
    }

    fun logout() {
        viewModelScope.launch {
            deleteToken()
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            val token = readToken()
            if (token == "") {
                _state.value = _state.value.copy(
                    tokenExpired = true
                )
                return@launch
            }

            try {
                getProfileUseCase("Bearer $token")
                    .collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _state.value = ProfileState(
                                    isLoading = false,
                                    profile = resource.data,
                                    error = ""
                                )
                            }

                            is Resource.Error -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = resource.message ?: "Gagal mengambil profil"
                                )
                            }

                            is Resource.Loading -> {
                                _state.value = _state.value.copy(
                                    isLoading = true,
                                    error = ""
                                )
                            }
                        }
                    }

                getProfilePhotoUseCase(photoPath = _state.value.profile?.profilePhoto ?: "")
                    .collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _profilePhoto.value = _profilePhoto.value.copy(
                                    isLoading = false,
                                    profilePhoto = resource.data,
                                    error = ""
                                )
                            }

                            is Resource.Error -> {
                                _profilePhoto.value = _profilePhoto.value.copy(
                                    isLoading = false,
                                    error = "Fetch foto profil gagal"
                                )
                            }

                            is Resource.Loading -> {
                                _profilePhoto.value = _profilePhoto.value.copy(
                                    isLoading = true,
                                    error = ""
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat merequest profil"
                )
            }
        }
    }

    private fun getStats() {
        viewModelScope.launch {
            _userStats.value = _userStats.value.copy(
                stats = Stats(
                    numberOfSongs = getAllSongsUseCase().first().size,
                    numberOfLikedSongs = getAllFavoritedSongsUseCase().first().size,
                    numberOfSongsListened = getAllListenedSongsUseCase().first().size
                )
            )
        }
    }

    fun getNumberOfSongs(): String {
        return _userStats.value.stats?.numberOfSongs.toString()
    }

    fun getNumberOfFavoritedSongs(): String {
        return _userStats.value.stats?.numberOfLikedSongs.toString()
    }

    fun getNumberOfListenedSongs(): String {
        return _userStats.value.stats?.numberOfSongsListened.toString()
    }
}
