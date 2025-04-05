package com.tubes1.purritify.features.profile.domain.usecase.getprofile;

import com.tubes1.purritify.core.common.utils.Resource
import com.tubes1.purritify.features.profile.data.remote.dto.toProfile
import com.tubes1.purritify.features.profile.domain.model.Profile
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository;
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class GetProfileUseCase (
    private val repository: ProfileRepository
) {
    operator fun invoke(authorization: String): Flow<Resource<Profile>> = flow {
        try {
            emit(Resource.Loading<Profile>())
            val profile = repository.getProfile(authorization).toProfile()
            emit(Resource.Success<Profile>(profile))
        }
        catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan tidak terduga."))
        }
        catch (e: IOException) {
            emit(Resource.Error("Tidak dapat mencapai server. Mohon periksa koneksi internet Anda."))
        }
    }
}

