package com.tubes1.purritify.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.auth.domain.usecase.auth.RequestTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

fun isEmailValid(email: String): Boolean {
    val EMAIL_ADDRESS_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
    return EMAIL_ADDRESS_PATTERN.matcher(email).matches()
}

class LoginStateViewModel(
    private val requestTokenUseCase: RequestTokenUseCase,
): ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun sendLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Email atau password tidak boleh kosong."
            )
            return
        } else if (!isEmailValid(email)) {
            _state.value = _state.value.copy(
                error = "Format email salah."
            )
            return
        } else if (password.length < 8) {
            _state.value = _state.value.copy(
                error = "Email atau password salah."
            )
            return
        }

        viewModelScope.launch {
            try {
                requestTokenUseCase(email, password)
                    .collect { resource ->
                        when(resource) {
                            is Resource.Success -> {
                                _state.value = LoginState(
                                    isLoading = false,
                                    isSuccess = true,
                                    token = resource.data,
                                    error = ""
                                )
                            }
                            is Resource.Error -> {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = resource.message ?: "Login gagal"
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
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Terjadi kesalahan saat login"
                )
            }
        }
    }
}