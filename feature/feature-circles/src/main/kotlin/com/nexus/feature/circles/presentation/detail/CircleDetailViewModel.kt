package com.nexus.feature.circles.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.NewPostPayload
import com.nexus.core.common.event.PostChange
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.domain.repository.CirclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CircleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CirclesRepository,
    private val eventBus: NewPostEventBus,
) : ViewModel() {

    // Type-safe routes store their arguments under the property name
    private val circleId: String = checkNotNull(savedStateHandle.get<String>("circleId")) { "circleId missing" }

    private val _state = MutableStateFlow(CircleDetailState(currentUid = repository.currentUid))
    val state: StateFlow<CircleDetailState> = _state.asStateFlow()

    private val _events = Channel<CircleDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var postsJob: Job? = null
    private var membersJob: Job? = null

    // Set when this user is the reason the circle disappears, so the listener's
    // "deleted" or "no access" that follows is not reported as something that happened to them
    private var closing = false
    private var closed = false

    init {
        observeCircle()
        observePostEvents()
    }

    fun onIntent(intent: CircleDetailIntent) {
        when (intent) {
            is CircleDetailIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            CircleDetailIntent.Refresh -> refresh()
            CircleDetailIntent.RetryPosts -> loadPosts()
            CircleDetailIntent.Leave -> leave()
            CircleDetailIntent.Delete -> delete()
            is CircleDetailIntent.RemoveMember -> removeMember(intent.uid)
            CircleDetailIntent.RegenerateCode -> regenerateCode()
            is CircleDetailIntent.RetryPending -> retryPending(intent.tempId)
            is CircleDetailIntent.DiscardPending -> discardPending(intent.tempId)
        }
    }

    private fun observeCircle() {
        viewModelScope.launch {
            try {
                repository.observeCircle(circleId).collect { circle ->
                    if (circle == null) {
                        close("Este círculo ya no existe")
                        return@collect
                    }
                    val previous = _state.value.circle
                    _state.update { it.copy(circle = circle, isLoading = false) }
                    if (previous == null) loadPosts()
                    if (previous == null || previous.memberIds != circle.memberIds) loadMembers(circle, refresh = false)
                    // Removed by the owner while the screen was open
                    val me = _state.value.currentUid
                    if (me != null && me !in circle.memberIds) close("Ya no formas parte de este círculo")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Reading the circle is denied once the user is no longer in it
                Timber.w(e, "Circle listener failed")
                close("Ya no formas parte de este círculo")
            }
        }
    }

    private fun refresh() {
        val circle = _state.value.circle ?: return
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            coroutineScope {
                launch { loadPostsNow() }
                launch { loadMembersNow(circle, refresh = true) }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch { loadPostsNow() }
    }

    private suspend fun loadPostsNow() {
        _state.update { it.copy(postsLoading = it.posts.isEmpty(), postsError = null) }
        try {
            val posts = repository.circlePosts(circleId)
            _state.update { it.copy(posts = posts, postsLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load circle posts")
            _state.update { it.copy(postsLoading = false, postsError = "No se pudieron cargar las publicaciones") }
        }
    }

    private fun loadMembers(circle: CircleSummary, refresh: Boolean) {
        membersJob?.cancel()
        membersJob = viewModelScope.launch { loadMembersNow(circle, refresh) }
    }

    private suspend fun loadMembersNow(circle: CircleSummary, refresh: Boolean) {
        try {
            val members = repository.members(circle, refresh)
            _state.update { it.copy(members = members, membersLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load circle members")
            _state.update { it.copy(membersLoading = false) }
        }
    }

    private fun leave() {
        val circle = _state.value.circle ?: return
        if (_state.value.isOwner) return
        runBusy("No se pudo salir del círculo. Revisa tu conexión.") {
            closing = true
            try {
                repository.leave(circleId)
            } catch (e: Exception) {
                closing = false
                throw e
            }
            close("Saliste de ${circle.name}", force = true)
        }
    }

    private fun delete() {
        val circle = _state.value.circle ?: return
        if (!_state.value.isOwner) return
        runBusy("No se pudo eliminar el círculo. Revisa tu conexión.") {
            closing = true
            try {
                repository.deleteCircle(circle)
            } catch (e: Exception) {
                closing = false
                throw e
            }
            close("Eliminaste ${circle.name}", force = true)
        }
    }

    private fun removeMember(uid: String) {
        val circle = _state.value.circle ?: return
        if (!_state.value.isOwner || uid == circle.ownerId) return
        val name = _state.value.members.firstOrNull { it.uid == uid }?.name ?: "La persona"
        runBusy("No se pudo quitar a $name. Revisa tu conexión.") {
            repository.removeMember(circle, uid)
            _events.send(CircleDetailEvent.ShowMessage("$name ya no está en el círculo. El código de invitación cambió."))
        }
    }

    private fun regenerateCode() {
        val circle = _state.value.circle ?: return
        if (!_state.value.isOwner) return
        runBusy("No se pudo cambiar el código. Revisa tu conexión.") {
            val code = repository.regenerateCode(circle)
            _events.send(CircleDetailEvent.ShowMessage("Nuevo código: ${InviteCode.format(code)}. El anterior ya no sirve."))
        }
    }

    private fun runBusy(errorMessage: String, block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Circle action failed")
                _events.send(CircleDetailEvent.ShowMessage(errorMessage))
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun close(message: String, force: Boolean = false) {
        if (closed || (closing && !force)) return
        closed = true
        _events.send(CircleDetailEvent.Closed(message))
    }

    private fun observePostEvents() {
        viewModelScope.launch {
            eventBus.events.collect(::onPostPayload)
        }
        viewModelScope.launch {
            eventBus.postChanges.collect { change ->
                when (change) {
                    is PostChange.Deleted -> _state.update { s -> s.copy(posts = s.posts.filterNot { it.id == change.postId }) }
                    is PostChange.Edited, is PostChange.Reacted -> if (_state.value.posts.any { it.id == change.postId }) loadPosts()
                }
            }
        }
    }

    private fun onPostPayload(payload: NewPostPayload) {
        when {
            payload.isPending -> {
                if (payload.circleId != circleId) return
                val pending = PendingCirclePost(
                    tempId = payload.tempId,
                    authorName = payload.authorName,
                    authorPhotoUrl = payload.authorPhotoUrl,
                    content = payload.content,
                    createdAt = payload.createdAt,
                )
                _state.update { it.copy(pending = listOf(pending) + it.pending) }
            }
            // Status updates carry only the temp id: they matter if it is one of ours
            _state.value.pending.none { it.tempId == payload.tempId } -> Unit
            payload.confirmedId == null -> _state.update { s ->
                s.copy(pending = s.pending.map { if (it.tempId == payload.tempId) it.copy(failed = true) else it })
            }
            else -> {
                _state.update { s -> s.copy(pending = s.pending.filterNot { it.tempId == payload.tempId }) }
                loadPosts()
            }
        }
    }

    private fun retryPending(tempId: String) {
        _state.update { s -> s.copy(pending = s.pending.map { if (it.tempId == tempId) it.copy(failed = false) else it }) }
        viewModelScope.launch { eventBus.requestRetry(tempId) }
    }

    private fun discardPending(tempId: String) {
        _state.update { s -> s.copy(pending = s.pending.filterNot { it.tempId == tempId }) }
        viewModelScope.launch { eventBus.requestDiscard(tempId) }
    }
}
