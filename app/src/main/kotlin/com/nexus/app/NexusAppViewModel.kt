package com.nexus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.datastore.UserSessionDataStore
import com.nexus.feature.auth.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NexusAppViewModel @Inject constructor(
    private val sessionDataStore: UserSessionDataStore,
    observeAuthState: ObserveAuthStateUseCase,
) : ViewModel() {

    // Estado reactivo: emite true/false según Firebase Auth (incluye cierre de sesión manual)
    val isLoggedIn = observeAuthState()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // Valor inicial desde DataStore (síncrono vía StateFlow) — sin parpadeo
            initialValue = false,
        )

    // Flujo del DataStore para determinar el startDestination antes de que Firebase responda
    val sessionFlow = sessionDataStore.sessionFlow
        .map { it?.isLoggedIn == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )
}
