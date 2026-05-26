package com.nexus.feature.auth.domain.repository

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authStateFlow: Flow<User?>
    suspend fun signInWithGoogle(idToken: String): NexusResult<User>
    suspend fun signInWithEmail(email: String, password: String): NexusResult<User>
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): NexusResult<User>
    suspend fun signOut(): NexusResult<Unit>
    suspend fun sendEmailVerification(): NexusResult<Unit>
}
