package com.nexus.feature.auth.data.datasource

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexus.core.common.result.NexusException
import com.nexus.feature.auth.data.model.UserDto
import com.nexus.feature.auth.data.model.toDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val USERS = "users"

class AuthRemoteDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
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
        val user = result.user ?: throw NexusException.UserNotFoundException()
        upsertUserProfile(user, user.displayName.orEmpty())
        return user.toDto()
    }

    override suspend fun signInWithEmail(email: String, password: String): UserDto {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw NexusException.UserNotFoundException()
        // The rules check post/story authors against users/{uid}: accounts created before
        // the profile existed (or whose profile write failed) get it on their next sign-in
        upsertUserProfile(user, user.displayName.orEmpty())
        return user.toDto()
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): UserDto {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw NexusException.UserNotFoundException()

        // The account already exists at this point: the remaining steps must not turn a
        // successful sign-up into an error (retrying would fail with "email already in use")
        runNonFatal("update display name") {
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdate).await()
        }
        runNonFatal("send email verification") { user.sendEmailVerification().await() }
        upsertUserProfile(user, displayName)

        return user.toDto().copy(displayName = displayName)
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        // Otherwise Credential Manager silently reuses the last Google account
        runNonFatal("clear credential state") {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    override suspend fun sendEmailVerification() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
            ?: throw NexusException.UserNotFoundException()
    }

    override suspend fun updateDisplayName(displayName: String) {
        val user = firebaseAuth.currentUser ?: throw NexusException.UserNotFoundException()
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdate).await()
        upsertUserProfile(user, displayName)
    }

    // Public profile in users/{uid}; the email is not stored because every signed-in user can read it
    private suspend fun upsertUserProfile(user: FirebaseUser, displayName: String) =
        runNonFatal("upsert user profile") {
            val profile = mapOf(
                "uid" to user.uid,
                "displayName" to displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            firestore.collection(USERS).document(user.uid)
                .set(profile, SetOptions.merge())
                .await()
        }

    private suspend fun runNonFatal(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not $action")
        }
    }
}
