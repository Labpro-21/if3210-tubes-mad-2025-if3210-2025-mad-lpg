package com.tubes1.purritify.features.profile.presentation.profiledetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.DeleteToken
import com.tubes1.purritify.core.common.utils.ReadToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileDetailViewModel(
    private val readToken: ReadToken,
    private val deleteToken: DeleteToken
): ViewModel() {
    private val _state = MutableStateFlow(ProfileDetailState())
    val state: StateFlow<ProfileDetailState> = _state.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            deleteToken()
        }
    }

    fun takeToken() {
        viewModelScope.launch {
            val token = readToken()
            Log.d("TOKEN_PRINT", "Token: $token")
            if (token == "") {
                _state.value = _state.value.copy(
                    tokenExpired = true
                )
                return@launch
            }
            // another process
        }
    }
//    fun printToken() { // Contoh lain
//        viewModelScope.launch {
//            try {
//                val token = readToken()
//                Log.d("TOKEN_PRINT", "Token: $token")
//            } catch(e: Error) {
//                Log.d("Hai","Hai")
//                _shouldRedirectToLogin.value = true
//            }
//        }
//    }


}
