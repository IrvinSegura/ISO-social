package com.nexus.feature.auth.data.model

import com.google.firebase.auth.FirebaseUser

data class UserDto(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val providerId: String,
)

fun FirebaseUser.toDto(): UserDto = UserDto(
    uid = uid,
    email = email.orEmpty(),
    displayName = displayName.orEmpty(),
    photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified,
    providerId = providerData.firstOrNull()?.providerId.orEmpty(),
)
