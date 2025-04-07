package com.tubes1.purritify.features.auth.domain.usecase.login

import android.content.Context
import androidx.datastore.dataStore
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.auth.data.remote.dto.LoginRequest
import com.tubes1.purritify.features.auth.data.remote.dto.toToken
import com.tubes1.purritify.features.auth.domain.model.Token
import com.tubes1.purritify.features.auth.domain.model.TokenSerializer
import com.tubes1.purritify.features.auth.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

private val Context.dataStore by dataStore(
    fileName = "user-preferences",
    serializer = TokenSerializer
)

class GetTokenUseCase (
    private val repository: LoginRepository,
    private val context: Context
) {
    operator fun invoke(email: String, password: String): Flow<Resource<Token>> = flow {
        try {
            emit(Resource.Loading<Token>())
            val token = repository.login(LoginRequest(email, password)).toToken()
            context.dataStore.updateData { token }
            emit(Resource.Success<Token>(token))
        }
        catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan tidak terduga."))
        }
        catch (e: IOException) {
            emit(Resource.Error("Tidak dapat mencapai server. Mohon periksa koneksi internet Anda."))
        }
    }
}