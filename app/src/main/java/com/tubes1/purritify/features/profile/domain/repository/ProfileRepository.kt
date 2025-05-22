package com.tubes1.purritify.features.profile.domain.repository;

import com.tubes1.purritify.features.profile.data.remote.dto.ProfileDto;
import okhttp3.MultipartBody
import okhttp3.ResponseBody;

interface ProfileRepository {

    suspend fun getProfile(authorization: String): ProfileDto

    suspend fun getProfilePhoto(profilePhotoPath: String): ResponseBody

    suspend fun editProfile(authorization: String, location: String?, profilePhoto: MultipartBody.Part?): ResponseBody
}
