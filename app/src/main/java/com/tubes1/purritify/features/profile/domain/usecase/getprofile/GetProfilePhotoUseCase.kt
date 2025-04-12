package com.tubes1.purritify.features.profile.domain.usecase.getprofile

import android.util.Log
import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.profile.data.remote.dto.toProfile
import com.tubes1.purritify.features.profile.domain.model.Profile
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

class GetProfilePhotoUseCase(
    private val repository: ProfileRepository
) {
    operator fun invoke(photoPath: String): Flow<Resource<ResponseBody>> =  flow {
        try {
            emit(Resource.Loading<ResponseBody>())
            val responseImage = repository.getProfilePhoto(photoPath)
            emit(Resource.Success<ResponseBody>(responseImage))
        } catch (e: HttpException) {
            Log.e(
                "GetProfilePhotoUseCase",
                "Error fetching profile photo data: ${e.localizedMessage}"
            )
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan tidak terduga."))
        } catch (e: IOException) {
            Log.e(
                "GetProfilePhotoUseCase",
                "Error fetching profile photo data: ${e.localizedMessage}"
            )
            emit(Resource.Error("Tidak dapat mencapai server. Mohon periksa koneksi internet Anda."))
        }
    }
}