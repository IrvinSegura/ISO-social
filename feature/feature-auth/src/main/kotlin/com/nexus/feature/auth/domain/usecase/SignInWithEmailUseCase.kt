package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import com.nexus.feature.auth.domain.validation.AuthValidator
import com.nexus.feature.auth.domain.validation.ValidationResult
import com.nexus.core.common.result.NexusException
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): NexusResult<User> {
        val emailValidation = AuthValidator.validateEmail(email)
        if (emailValidation is ValidationResult.Invalid) {
            return NexusResult.Error(NexusException.InvalidCredentialsException())
        }
        if (password.isBlank()) {
            return NexusResult.Error(NexusException.InvalidCredentialsException())
        }
        return repository.signInWithEmail(email.trim(), password)
    }
}
