package com.nexus.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.usecase.SignInWithEmailUseCase
import com.nexus.feature.auth.domain.usecase.SignInWithGoogleUseCase
import com.nexus.feature.auth.domain.validation.AuthValidator
import com.nexus.feature.auth.domain.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> updateEmail(intent.value)
            is LoginIntent.UpdatePassword -> updatePassword(intent.value)
            is LoginIntent.TogglePasswordVisibility -> togglePasswordVisibility()
            is LoginIntent.SignIn -> signInWithEmailPassword()
            is LoginIntent.SignInWithGoogle -> handleGoogleSignIn(intent.idToken)
            is LoginIntent.NavigateToRegister -> sendEvent(LoginEvent.NavigateToRegister)
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    private fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun signInWithEmailPassword() {
        val state = _uiState.value
        val emailError = (AuthValidator.validateEmail(state.email) as? ValidationResult.Invalid)?.message
        val passwordError = if (state.password.isBlank()) "Password is required." else null

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true) }
            when (val result = signInWithEmail(state.email, state.password)) {
                is NexusResult.Success -> sendEvent(LoginEvent.NavigateToHome)
                is NexusResult.Error -> sendEvent(LoginEvent.ShowError(result.exception.message.orEmpty()))
            }
            _uiState.update { it.copy(isSigningIn = false) }
        }
    }

    private fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleSigningIn = true) }
            when (val result = signInWithGoogle(idToken)) {
                is NexusResult.Success -> sendEvent(LoginEvent.NavigateToHome)
                is NexusResult.Error -> sendEvent(LoginEvent.ShowError(result.exception.message.orEmpty()))
            }
            _uiState.update { it.copy(isGoogleSigningIn = false) }
        }
    }

    private fun sendEvent(event: LoginEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
