package com.nexus.feature.auth.presentation.login

import androidx.compose.runtime.Immutable

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSigningIn: Boolean = false,
    val isGoogleSigningIn: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
) {
    val isLoading: Boolean get() = isSigningIn || isGoogleSigningIn
}

sealed interface LoginIntent {
    data class UpdateEmail(val value: String) : LoginIntent
    data class UpdatePassword(val value: String) : LoginIntent
    data object TogglePasswordVisibility : LoginIntent
    data object SignIn : LoginIntent
    data class SignInWithGoogle(val idToken: String) : LoginIntent
    data object NavigateToRegister : LoginIntent
}

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data object NavigateToRegister : LoginEvent
    data class ShowError(val message: String) : LoginEvent
}
