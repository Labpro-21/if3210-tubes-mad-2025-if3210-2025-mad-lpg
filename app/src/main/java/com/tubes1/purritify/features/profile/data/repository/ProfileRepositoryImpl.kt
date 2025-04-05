package com.tubes1.purritify.features.profile.data.repository

import com.tubes1.purritify.features.profile.data.remote.ProfileApi
import com.tubes1.purritify.features.profile.data.remote.dto.ProfileDto
import com.tubes1.purritify.features.profile.domain.repository.ProfileRepository
import okhttp3.ResponseBody

class ProfileRepositoryImpl (
    private val api: ProfileApi
) : ProfileRepository {

    override suspend fun getProfile(authorization: String): ProfileDto {
        return api.getProfile(authorization)
    }

    override suspend fun getProfilePhoto(profilePhotoPath: String): ResponseBody {
        return api.getProfilePhoto(profilePhotoPath)
    }

}