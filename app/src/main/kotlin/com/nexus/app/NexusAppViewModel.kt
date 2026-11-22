package com.nexus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.datastore.AppPreferencesDataStore
import com.nexus.core.datastore.model.ThemeMode
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.usecase.ObserveAuthStateUseCase
import com.nexus.feature.auth.domain.usecase.SignOutUseCase
import com.nexus.feature.feed.domain.usecase.ClearFeedCacheUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class AuthStatus { Loading, LoggedIn, LoggedOut }

@HiltViewModel
class NexusAppViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    preferences: AppPreferencesDataStore,
    private val clearFeedCache: ClearFeedCacheUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    // Firebase Auth is the single source of truth. Its listener fires right away with the
    // locally cached user, so the splash screen only stays up for a few milliseconds.
    val authStatus: StateFlow<AuthStatus> = observeAuthState()
        .map { user -> if (user != null) AuthStatus.LoggedIn else AuthStatus.LoggedOut }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthStatus.Loading,
        )

    // null until DataStore answers: the splash stays up meanwhile, so a dark-mode user
    // never sees one light frame
    val themeMode: StateFlow<ThemeMode?> = preferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    init {
        // Covers sign-out and a revoked session alike: the feed cache holds per-user data
        // (myReaction), so the next user must start from an empty cache
        viewModelScope.launch {
            authStatus.filter { it == AuthStatus.LoggedOut }.collect { clearFeedCache() }
        }
    }

    /** Ends the session; [authStatus] then turns LoggedOut and navigation goes back to login. */
    fun signOut() {
        viewModelScope.launch {
            val result = signOutUseCase()
            if (result is NexusResult.Error) Timber.w(result.exception, "Sign-out failed")
        }
    }
}
