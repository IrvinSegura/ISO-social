package com.nexus.feature.auth.data.mapper

import com.nexus.feature.auth.data.model.UserDto
import com.nexus.feature.auth.domain.model.AuthProvider
import com.nexus.feature.auth.domain.model.User

private const val GOOGLE_PROVIDER_ID = "google.com"

fun UserDto.toDomain(): User = User(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isEmailVerified = isEmailVerified,
    provider = if (providerId == GOOGLE_PROVIDER_ID) AuthProvider.GOOGLE else AuthProvider.EMAIL,
)
