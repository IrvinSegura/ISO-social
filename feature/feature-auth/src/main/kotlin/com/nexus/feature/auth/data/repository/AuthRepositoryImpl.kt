package com.nexus.feature.auth.data.repository

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.core.datastore.UserSessionDataStore
import com.nexus.core.datastore.model.UserSession
import com.nexus.feature.auth.data.datasource.AuthRemoteDataSource
import com.nexus.feature.auth.data.mapper.toDomain
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionDataStore: UserSessionDataStore,
) : AuthRepository {

    override val authStateFlow: Flow<User?> =
        remoteDataSource.authStateFlow.map { dto -> dto?.toDomain() }

    override suspend fun signInWithGoogle(idToken: String): NexusResult<User> =
        executeAuth { remoteDataSource.signInWithGoogle(idToken).toDomain() }

    override suspend fun signInWithEmail(email: String, password: String): NexusResult<User> =
        executeAuth { remoteDataSource.signInWithEmail(email, password).toDomain() }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): NexusResult<User> = executeAuth {
        remoteDataSource.registerWithEmail(email, password, displayName).toDomain()
    }

    override suspend fun signOut(): NexusResult<Unit> = try {
        remoteDataSource.signOut()
        sessionDataStore.clearSession()
        NexusResult.Success(Unit)
    } catch (e: Exception) {
        NexusResult.Error(NexusException.UnknownException(e))
    }

    override suspend fun sendEmailVerification(): NexusResult<Unit> = try {
        remoteDataSource.sendEmailVerification()
        NexusResult.Success(Unit)
    } catch (e: Exception) {
        NexusResult.Error(e.toNexusException())
    }

    private suspend fun executeAuth(block: suspend () -> User): NexusResult<User> = try {
        val user = block()
        sessionDataStore.saveSession(user.toSession())
        NexusResult.Success(user)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        NexusResult.Error(NexusException.InvalidCredentialsException())
    } catch (e: FirebaseAuthUserCollisionException) {
        NexusResult.Error(NexusException.EmailAlreadyInUseException())
    } catch (e: FirebaseAuthWeakPasswordException) {
        NexusResult.Error(NexusException.WeakPasswordException())
    } catch (e: NexusException) {
        NexusResult.Error(e)
    } catch (e: Exception) {
        NexusResult.Error(NexusException.UnknownException(e))
    }

    private fun Exception.toNexusException(): NexusException = when (this) {
        is FirebaseAuthInvalidCredentialsException -> NexusException.InvalidCredentialsException()
        is FirebaseAuthUserCollisionException -> NexusException.EmailAlreadyInUseException()
        is FirebaseAuthWeakPasswordException -> NexusException.WeakPasswordException()
        is NexusException -> this
        else -> NexusException.UnknownException(this)
    }

    private fun User.toSession() = UserSession(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl.orEmpty(),
        isLoggedIn = true,
    )
}
