package com.nexus.feature.auth.data.model

import com.google.firebase.auth.FirebaseUser

private const val FIREBASE_PROVIDER_ID = "firebase"

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
    // providerData[0] is always the "firebase" entry; look for the real sign-in provider
    providerId = providerData
        .map { it.providerId }
        .firstOrNull { it != FIREBASE_PROVIDER_ID }
        .orEmpty(),
)
