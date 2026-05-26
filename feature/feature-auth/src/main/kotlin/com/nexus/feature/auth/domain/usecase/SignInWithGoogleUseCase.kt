package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): NexusResult<User> =
        repository.signInWithGoogle(idToken)
}
