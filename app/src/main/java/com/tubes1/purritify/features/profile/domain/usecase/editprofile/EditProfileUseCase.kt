package com.tubes1.purritify.features.profile.domain.usecase.editprofile

import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

class EditProfileUseCase(
    private val repository: ProfileRepository
) {
    operator fun invoke(authorization: String, location: String?, profilePhoto: MultipartBody.Part?): Flow<Resource<ResponseBody>> =
        flow {
            emit(Resource.Loading<ResponseBody>())
            val response = repository.editProfile(authorization, location, profilePhoto)
            emit(Resource.Success<ResponseBody>(response))
        }.catch { e ->
            val message = when (e) {
                is HttpException -> when (e.code()) {
                    401 -> "Token tidak valid."
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