package com.nexus.feature.auth.data.datasource

import com.nexus.feature.auth.data.model.UserDto
import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {
    val authStateFlow: Flow<UserDto?>
    suspend fun signInWithGoogle(idToken: String): UserDto
    suspend fun signInWithEmail(email: String, password: String): UserDto
    suspend fun registerWithEmail(email: String, password: String, displayName: String): UserDto
    suspend fun signOut()
    suspend fun sendEmailVerification()
    suspend fun updateDisplayName(displayName: String)
}
