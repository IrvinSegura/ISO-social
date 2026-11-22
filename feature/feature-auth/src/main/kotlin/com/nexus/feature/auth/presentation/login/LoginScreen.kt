package com.nexus.feature.auth.presentation.login

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import timber.log.Timber
import com.nexus.core.ui.components.GoogleSignInButton
import com.nexus.core.ui.components.NexusButton
import com.nexus.core.ui.components.NexusTextField
import com.nexus.feature.auth.core.AuthConstants
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onNavigateToHome()
                is LoginEvent.NavigateToRegister -> onNavigateToRegister()
                is LoginEvent.ShowError -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                LoginHeader()

                Spacer(modifier = Modifier.height(48.dp))

                NexusTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onIntent(LoginIntent.UpdateEmail(it)) },
                    label = "Email",
                    leadingIcon = Icons.Rounded.Email,
                    error = uiState.emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                NexusTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
                    label = "Password",
                    leadingIcon = Icons.Rounded.Lock,
                    trailingIcon = if (uiState.isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    onTrailingIconClick = { viewModel.onIntent(LoginIntent.TogglePasswordVisibility) },
                    isPassword = true,
                    isPasswordVisible = uiState.isPasswordVisible,
                    error = uiState.passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onIntent(LoginIntent.SignIn)
                        },
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                NexusButton(
                    text = "Sign In",
                    onClick = { viewModel.onIntent(LoginIntent.SignIn) },
                    isLoading = uiState.isSigningIn,
                    enabled = !uiState.isLoading,
                )

                Spacer(modifier = Modifier.height(20.dp))

                OrDivider()

                Spacer(modifier = Modifier.height(20.dp))

                GoogleSignInButton(
                    onClick = { launchGoogleSignIn(activity, scope, snackbarHostState) { idToken ->
                        viewModel.onIntent(LoginIntent.SignInWithGoogle(idToken))
                    }},
                    isLoading = uiState.isGoogleSigningIn,
                    enabled = !uiState.isLoading,
                )

                Spacer(modifier = Modifier.height(32.dp))

                RegisterPrompt(
                    onNavigateToRegister = { viewModel.onIntent(LoginIntent.NavigateToRegister) },
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LoginHeader() {
    Text(
        text = "ISO Social",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Welcome back",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Sign in to continue your journey",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "  or  ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RegisterPrompt(onNavigateToRegister: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Don't have an account?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onNavigateToRegister) {
            Text(
                text = "Register",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun launchGoogleSignIn(
    activity: Activity,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onTokenReceived: (String) -> Unit,
) {
    val credentialManager = CredentialManager.create(activity)
    // The "Sign in with Google" option is the one meant for an explicit button: unlike
    // GetGoogleIdOption (the one-tap sheet), it also works when the device has no Google
    // account yet, letting the user add one instead of failing with NoCredentialException
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(AuthConstants.GOOGLE_WEB_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                onTokenReceived(googleCredential.idToken)
            } else {
                Timber.w("Unexpected credential type: %s", credential.type)
                snackbarHostState.showSnackbar("No se pudo iniciar sesión con Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            // Usually the user closed the dialog, so no snackbar. Play Services also reports
            // config errors (e.g. UNREGISTERED_ON_API_CONSOLE: SHA-1 missing) as a cancellation,
            // so it is still logged for debugging
            Timber.d(e, "Google sign-in cancelled: %s", e.message)
        } catch (e: NoCredentialException) {
            Timber.w(e, "Google sign-in: no credential")
            snackbarHostState.showSnackbar("Agrega una cuenta de Google al dispositivo e inténtalo de nuevo.")
        } catch (e: GetCredentialException) {
            Timber.e(e, "Google sign-in failed: %s", e.type)
            snackbarHostState.showSnackbar("No se pudo iniciar sesión con Google. Inténtalo de nuevo.")
        } catch (e: GoogleIdTokenParsingException) {
            Timber.e(e, "Google sign-in: invalid ID token")
            snackbarHostState.showSnackbar("No se pudo iniciar sesión con Google. Inténtalo de nuevo.")
        }
    }
}
