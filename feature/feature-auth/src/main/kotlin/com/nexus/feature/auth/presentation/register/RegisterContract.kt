package com.nexus.feature.auth.presentation.register

import androidx.compose.runtime.Immutable

@Immutable
data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
)

sealed interface RegisterIntent {
    data class UpdateDisplayName(val value: String) : RegisterIntent
    data class UpdateEmail(val value: String) : RegisterIntent
    data class UpdatePassword(val value: String) : RegisterIntent
    data class UpdateConfirmPassword(val value: String) : RegisterIntent
    data object TogglePasswordVisibility : RegisterIntent
    data object ToggleConfirmPasswordVisibility : RegisterIntent
    data object Register : RegisterIntent
    data object NavigateToLogin : RegisterIntent
}

sealed interface RegisterEvent {
    data object NavigateToHome : RegisterEvent
    data object NavigateToLogin : RegisterEvent
    data class ShowError(val message: String) : RegisterEvent
}
