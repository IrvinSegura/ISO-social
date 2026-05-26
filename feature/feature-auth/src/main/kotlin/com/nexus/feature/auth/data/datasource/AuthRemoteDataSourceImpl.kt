package com.nexus.feature.auth.data.datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.nexus.core.common.result.NexusException
import com.nexus.feature.auth.data.model.UserDto
import com.nexus.feature.auth.data.model.toDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRemoteDataSource {

    override val authStateFlow: Flow<UserDto?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDto())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): UserDto {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user?.toDto() ?: throw NexusException.UserNotFoundException()
    }

    override suspend fun signInWithEmail(email: String, password: String): UserDto {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toDto() ?: throw NexusException.UserNotFoundException()
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): UserDto {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw NexusException.UserNotFoundException()
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdate).await()
        user.sendEmailVerification().await()
        return user.toDto().copy(displayName = displayName)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun sendEmailVerification() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
            ?: throw NexusException.UserNotFoundException()
    }

    override suspend fun updateDisplayName(displayName: String) {
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        firebaseAuth.currentUser?.updateProfile(profileUpdate)?.await()
            ?: throw NexusException.UserNotFoundException()
    }
}
