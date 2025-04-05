package com.tubes1.purritify.features.profile.domain.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class Profile(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val location: String
)