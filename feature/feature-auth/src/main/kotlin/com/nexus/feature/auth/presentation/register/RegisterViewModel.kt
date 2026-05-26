package com.nexus.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.feature.auth.domain.usecase.RegisterParams
import com.nexus.feature.auth.domain.usecase.RegisterResult
import com.nexus.feature.auth.domain.usecase.RegisterValidationError
import com.nexus.feature.auth.domain.usecase.RegisterWithEmailUseCase
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
class RegisterViewModel @Inject constructor(
    private val registerWithEmail: RegisterWithEmailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateDisplayName -> _uiState.update {
                it.copy(displayName = intent.value, displayNameError = null)
            }
            is RegisterIntent.UpdateEmail -> _uiState.update {
                it.copy(email = intent.value, emailError = null)
            }
            is RegisterIntent.UpdatePassword -> _uiState.update {
                it.copy(password = intent.value, passwordError = null)
            }
            is RegisterIntent.UpdateConfirmPassword -> _uiState.update {
                it.copy(confirmPassword = intent.value, confirmPasswordError = null)
            }
            is RegisterIntent.TogglePasswordVisibility -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            is RegisterIntent.ToggleConfirmPasswordVisibility -> _uiState.update {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            is RegisterIntent.Register -> register()
            is RegisterIntent.NavigateToLogin -> sendEvent(RegisterEvent.NavigateToLogin)
        }
    }

    private fun register() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.clearErrors().copy(isLoading = true) }
            val result = registerWithEmail(
                RegisterParams(
                    email = state.email,
                    password = state.password,
                    confirmPassword = state.confirmPassword,
                    displayName = state.displayName,
                )
            )
            when (result) {
                is RegisterResult.Success -> sendEvent(RegisterEvent.NavigateToHome)
                is RegisterResult.ValidationErrors -> applyValidationErrors(result.errors)
                is RegisterResult.Failure -> sendEvent(RegisterEvent.ShowError(result.exception.message.orEmpty()))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun applyValidationErrors(errors: List<RegisterValidationError>) {
        _uiState.update { state ->
            var updated = state
            errors.forEach { error ->
                updated = when (error) {
                    is RegisterValidationError.DisplayNameError -> updated.copy(displayNameError = error.message)
                    is RegisterValidationError.EmailError -> updated.copy(emailError = error.message)
                    is RegisterValidationError.PasswordError -> updated.copy(passwordError = error.message)
                    is RegisterValidationError.ConfirmPasswordError -> updated.copy(confirmPasswordError = error.message)
                }
            }
            updated
        }
    }

    private fun RegisterUiState.clearErrors() = copy(
        displayNameError = null,
        emailError = null,
        passwordError = null,
        confirmPasswordError = null,
    )

    private fun sendEvent(event: RegisterEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
