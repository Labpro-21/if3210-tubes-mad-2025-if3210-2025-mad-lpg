package com.tubes1.purritify.features.auth.domain.usecase.token

import android.content.Context
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.core.data.local.userPreferencesDataStore
import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.toToken
import com.tubes1.purritify.features.auth.domain.model.Token
import com.tubes1.purritify.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class RequestTokenUseCase (
    private val repository: AuthRepository,
    private val context: Context
) {
    operator fun invoke(email: String, password: String): Flow<Resource<Token>> =
        flow {
            emit(Resource.Loading())
            val token = repository.login(LoginRequest(email, password)).toToken()
            val dataStore = context.userPreferencesDataStore
            dataStore.updateData { token }
            emit(Resource.Success(token))
        }.catch { e ->
            val message = when (e) {
                is HttpException -> when (e.code()) {
                    401 -> "Email atau password salah."
                    400 -> "Request tidak valid."
                    500 -> "Server error."
                    else -> e.localizedMessage ?: "Kesalahan tidak terduga."
                }
                is IOException -> "Periksa koneksi internet Anda."
                else -> e.localizedMessage ?: "Kesalahan tidak diketahui."
            }
            emit(Resource.Error(message))
        }
}