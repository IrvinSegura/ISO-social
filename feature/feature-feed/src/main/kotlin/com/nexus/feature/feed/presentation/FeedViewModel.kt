package com.nexus.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.model.Post
import com.nexus.feature.feed.domain.usecase.GetFeedUseCase
import com.nexus.feature.feed.domain.usecase.SendEcoUseCase
import com.nexus.feature.feed.domain.usecase.ToggleReactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    getFeedUseCase: GetFeedUseCase,
    private val sendEcoUseCase: SendEcoUseCase,
    private val toggleReactionUseCase: ToggleReactionUseCase,
    private val newPostEventBus: NewPostEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val posts: Flow<PagingData<Post>> = getFeedUseCase().cachedIn(viewModelScope)

    init {
        observeNewPosts()
    }

    private fun observeNewPosts() {
        viewModelScope.launch {
            newPostEventBus.events.collect { payload ->
                when {
                    // Nuevo post optimista → añadir al inicio inmediatamente
                    payload.isPending -> {
                        val pending = PendingPost(
                            tempId = payload.tempId,
                            authorName = payload.authorName,
                            authorPhotoUrl = payload.authorPhotoUrl,
                            content = payload.content,
                            mood = payload.mood,
                            locationName = payload.locationName,
                            createdAt = payload.createdAt,
                        )
                        _uiState.update { state ->
                            state.copy(pendingPosts = listOf(pending) + state.pendingPosts)
                        }
                    }
                    // Post fallido (confirmedId == null) → marcar FAILED para reintentar
                    payload.confirmedId == null -> {
                        _uiState.update { state ->
                            state.copy(
                                pendingPosts = state.pendingPosts.map { p ->
                                    if (p.tempId == payload.tempId) p.copy(status = PendingPostStatus.FAILED)
                                    else p
                                },
                            )
                        }
                    }
                    // Confirmado por servidor → quitar de pendientes
                    else -> {
                        _uiState.update { state ->
                            state.copy(
                                pendingPosts = state.pendingPosts.filter { it.tempId != payload.tempId },
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearPendingPosts() {
        _uiState.update { it.copy(pendingPosts = emptyList()) }
    }

    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.OpenEcoSheet -> _uiState.update { it.copy(ecoTargetPostId = intent.postId) }
            FeedIntent.CloseEcoSheet -> _uiState.update { it.copy(ecoTargetPostId = null) }
            is FeedIntent.SendEco -> sendEco(intent.postId, intent.message)
            is FeedIntent.ToggleReaction -> toggleReaction(intent.postId, intent.emoji)
            is FeedIntent.NavigateToPost -> viewModelScope.launch {
                _events.send(FeedEvent.NavigateToPost(intent.postId))
            }
            FeedIntent.NavigateToCreatePost -> viewModelScope.launch {
                _events.send(FeedEvent.NavigateToCreatePost)
            }
            FeedIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun sendEco(postId: String, message: String) {
        // Incremento optimista inmediato
        _uiState.update { state ->
            val deltas = state.ecoCountDeltas.toMutableMap()
            deltas[postId] = (deltas[postId] ?: 0) + 1
            state.copy(isEcoSending = true, ecoCountDeltas = deltas)
        }

        viewModelScope.launch {
            when (val result = sendEcoUseCase(postId, message)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isEcoSending = false, ecoTargetPostId = null) }
                    _events.send(FeedEvent.EcoSent)
                }
                is NexusResult.Error -> {
                    // Revertir delta si falla
                    _uiState.update { state ->
                        val deltas = state.ecoCountDeltas.toMutableMap()
                        val current = deltas[postId] ?: 1
                        if (current <= 1) deltas.remove(postId) else deltas[postId] = current - 1
                        state.copy(isEcoSending = false, ecoCountDeltas = deltas, error = result.exception.message)
                    }
                }
            }
        }
    }

    private fun toggleReaction(postId: String, emoji: String) {
        // Actualización optimista inmediata
        val previousReaction = _uiState.value.userReactions[postId]
        val newReactions = _uiState.value.userReactions.toMutableMap()
        if (previousReaction == emoji) {
            newReactions.remove(postId) // Quitar reacción
        } else {
            newReactions[postId] = emoji // Nueva o diferente reacción
        }
        _uiState.update { it.copy(userReactions = newReactions) }

        viewModelScope.launch {
            when (val result = toggleReactionUseCase(postId, emoji)) {
                is NexusResult.Success -> Unit
                is NexusResult.Error -> {
                    // Revertir si falla
                    val reverted = _uiState.value.userReactions.toMutableMap()
                    if (previousReaction != null) {
                        reverted[postId] = previousReaction
                    } else {
                        reverted.remove(postId)
                    }
                    _uiState.update { it.copy(userReactions = reverted, error = result.exception.message) }
                }
            }
        }
    }
}
