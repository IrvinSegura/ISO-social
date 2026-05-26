package com.nexus.feature.auth.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val provider: AuthProvider,
)
