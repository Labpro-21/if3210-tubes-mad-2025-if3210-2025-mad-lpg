package com.tubes1.purritify.features.profile.data.remote.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.tubes1.purritify.features.profile.domain.model.Profile
import java.time.LocalDateTime

data class ProfileDto(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String
)

fun ProfileDto.toProfile(): Profile {
    return Profile(
        id = id,
        username = username,
        email = email,
        profilePhoto = profilePhoto,
        location = location
    )
}