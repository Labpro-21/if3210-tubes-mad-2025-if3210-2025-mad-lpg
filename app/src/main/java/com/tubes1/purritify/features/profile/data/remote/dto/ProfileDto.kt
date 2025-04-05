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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val updatedAt: LocalDateTime
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