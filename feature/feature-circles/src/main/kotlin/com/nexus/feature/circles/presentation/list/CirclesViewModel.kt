package com.nexus.feature.circles.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.domain.repository.CirclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val repository: CirclesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CirclesState(currentUid = repository.currentUid))
    val state: StateFlow<CirclesState> = _state.asStateFlow()

    private val _events = Channel<CirclesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var observeJob: Job? = null

    init {
        observe()
    }

    fun onIntent(intent: CirclesIntent) {
        when (intent) {
            CirclesIntent.Retry -> observe()
            is CirclesIntent.Create -> create(intent.name, intent.emoji, intent.colorIndex)
            is CirclesIntent.LookUpCode -> lookUp(intent.input)
            CirclesIntent.ConfirmJoin -> join()
            CirclesIntent.ResetJoin -> _state.update { it.copy(join = JoinState()) }
        }
    }

    private fun observe() {
        observeJob?.cancel()
        _state.update { it.copy(isLoading = it.circles.isEmpty(), error = null) }
        observeJob = viewModelScope.launch {
            repository.observeMyCircles()
                .catch { e ->
                    Timber.w(e, "Circles listener failed")
                    _state.update { it.copy(isLoading = false, error = "No se pudieron cargar tus círculos") }
                }
                .collect { circles -> _state.update { it.copy(circles = circles, isLoading = false, error = null) } }
        }
    }

    private fun create(name: String, emoji: String, colorIndex: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_NAME || _state.value.isCreating) return
        _state.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            try {
                val id = repository.createCircle(trimmed, emoji, colorIndex)
                _events.send(CirclesEvent.OpenCircle(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not create a circle")
                _events.send(CirclesEvent.ShowMessage("No se pudo crear el círculo. Revisa tu conexión."))
            } finally {
                _state.update { it.copy(isCreating = false) }
            }
        }
    }

    private fun lookUp(input: String) {
        val code = InviteCode.normalize(input)
        if (!InviteCode.isValid(code)) {
            _state.update {
                it.copy(join = JoinState(error = "El código tiene ${InviteCode.LENGTH} letras y números, sin O, I, 0 ni 1"))
            }
            return
        }
        _state.update { it.copy(join = JoinState(isSearching = true)) }
        viewModelScope.launch {
            val join = try {
                val preview = repository.findInvite(code)
                if (preview == null) JoinState(error = "Ese código no existe o ya cambió. Pide uno nuevo.")
                else JoinState(preview = preview)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not look up an invite code")
                JoinState(error = "No se pudo buscar el código. Revisa tu conexión.")
            }
            _state.update { it.copy(join = join) }
        }
    }

    private fun join() {
        val current = _state.value
        val preview = current.join.preview ?: return
        if (current.join.isJoining) return
        // Already in it: nothing to write, just open it
        if (current.circles.any { it.id == preview.circleId }) {
            _state.update { it.copy(join = JoinState()) }
            viewModelScope.launch { _events.send(CirclesEvent.OpenCircle(preview.circleId)) }
            return
        }
        _state.update { it.copy(join = it.join.copy(isJoining = true, error = null)) }
        viewModelScope.launch {
            try {
                repository.join(preview)
                _state.update { it.copy(join = JoinState()) }
                _events.send(CirclesEvent.OpenCircle(preview.circleId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not join a circle")
                _state.update {
                    it.copy(
                        join = it.join.copy(
                            isJoining = false,
                            error = "No pudiste unirte. El círculo puede estar lleno " +
                                "(máx. ${CircleDirectory.MAX_MEMBERS}) o el código cambió.",
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val MAX_NAME = 40
    }
}
