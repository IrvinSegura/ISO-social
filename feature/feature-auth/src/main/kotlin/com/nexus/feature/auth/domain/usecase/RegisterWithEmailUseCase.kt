package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import com.nexus.feature.auth.domain.validation.AuthValidator
import com.nexus.feature.auth.domain.validation.ValidationResult
import javax.inject.Inject

data class RegisterParams(
    val email: String,
    val password: String,
    val confirmPassword: String,
    val displayName: String,
)

sealed interface RegisterValidationError {
    data class EmailError(val message: String) : RegisterValidationError
    data class PasswordError(val message: String) : RegisterValidationError
    data class ConfirmPasswordError(val message: String) : RegisterValidationError
    data class DisplayNameError(val message: String) : RegisterValidationError
}

sealed interface RegisterResult {
    data class Success(val user: User) : RegisterResult
    data class ValidationErrors(val errors: List<RegisterValidationError>) : RegisterResult
    data class Failure(val exception: NexusException) : RegisterResult
}

class RegisterWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(params: RegisterParams): RegisterResult {
        val errors = buildList {
            val nameResult = AuthValidator.validateDisplayName(params.displayName)
            if (nameResult is ValidationResult.Invalid) {
                add(RegisterValidationError.DisplayNameError(nameResult.message))
            }
            val emailResult = AuthValidator.validateEmail(params.email)
            if (emailResult is ValidationResult.Invalid) {
                add(RegisterValidationError.EmailError(emailResult.message))
            }
            val passwordResult = AuthValidator.validatePassword(params.password)
            if (passwordResult is ValidationResult.Invalid) {
                add(RegisterValidationError.PasswordError(passwordResult.message))
            }
            val confirmResult = AuthValidator.validateConfirmPassword(params.password, params.confirmPassword)
            if (confirmResult is ValidationResult.Invalid) {
                add(RegisterValidationError.ConfirmPasswordError(confirmResult.message))
            }
        }

        if (errors.isNotEmpty()) return RegisterResult.ValidationErrors(errors)

        return when (val result = repository.registerWithEmail(
            email = params.email.trim(),
            password = params.password,
            displayName = params.displayName.trim(),
        )) {
            is NexusResult.Success -> RegisterResult.Success(result.data)
            is NexusResult.Error -> RegisterResult.Failure(result.exception)
        }
    }
}
