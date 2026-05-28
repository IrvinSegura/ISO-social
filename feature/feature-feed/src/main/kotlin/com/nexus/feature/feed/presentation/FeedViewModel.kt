package com.nexus.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val posts: Flow<PagingData<Post>> = getFeedUseCase().cachedIn(viewModelScope)

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
        viewModelScope.launch {
            _uiState.update { it.copy(isEcoSending = true) }
            when (val result = sendEcoUseCase(postId, message)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isEcoSending = false, ecoTargetPostId = null) }
                    _events.send(FeedEvent.EcoSent)
                }
                is NexusResult.Error -> {
                    _uiState.update {
                        it.copy(isEcoSending = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    private fun toggleReaction(postId: String, emoji: String) {
        viewModelScope.launch {
            when (val result = toggleReactionUseCase(postId, emoji)) {
                is NexusResult.Success -> Unit
                is NexusResult.Error -> _uiState.update { it.copy(error = result.exception.message) }
            }
        }
    }
}
