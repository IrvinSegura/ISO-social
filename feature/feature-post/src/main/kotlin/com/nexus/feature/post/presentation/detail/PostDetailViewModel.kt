package com.nexus.feature.post.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.data.remote.GiphyApi
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.usecase.GetPostDetailUseCase
import com.nexus.feature.post.domain.usecase.SendEcoUseCase
import com.nexus.feature.post.presentation.navigation.PostDetailRoute
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
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val sendEcoUseCase: SendEcoUseCase,
    private val giphyApi: GiphyApi,
) : ViewModel() {

    private val postId: String = savedStateHandle.toRoute<PostDetailRoute>().postId

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadPost()
        searchGifs("")
    }

    fun onIntent(intent: PostDetailIntent) {
        when (intent) {
            PostDetailIntent.OpenEcoSheet -> _uiState.update { it.copy(ecoSheetOpen = true) }
            PostDetailIntent.CloseEcoSheet -> _uiState.update { it.copy(ecoSheetOpen = false) }
            is PostDetailIntent.SelectEcoType -> _uiState.update { it.copy(selectedEcoType = intent.type) }
            is PostDetailIntent.SendTextEco -> sendEco(EcoType.TEXT, intent.message, null, null)
            is PostDetailIntent.SendEmojiEco -> sendEco(EcoType.EMOJI, "", intent.emoji, null)
            is PostDetailIntent.SendMediaEco -> sendEco(intent.type, "", null, intent.mediaUrl)
            is PostDetailIntent.SearchGifs -> searchGifs(intent.query)
            PostDetailIntent.DismissError -> _uiState.update { it.copy(error = null) }
            PostDetailIntent.Retry -> loadPost()
        }
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getPostDetailUseCase(postId)) {
                is NexusResult.Success -> _uiState.update {
                    it.copy(isLoading = false, post = result.data)
                }
                is NexusResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun sendEco(
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEcoSending = true) }
            when (val result = sendEcoUseCase(postId, type, message, emoji, mediaUrl)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isEcoSending = false, ecoSheetOpen = false) }
                    _events.send(PostDetailEvent.EcoSent)
                    loadPost()
                }
                is NexusResult.Error -> _uiState.update {
                    it.copy(isEcoSending = false, error = result.exception.message)
                }
            }
        }
    }

    private fun searchGifs(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingGifs = true, gifSearchQuery = query) }
            try {
                val response = if (query.isBlank()) {
                    giphyApi.trending()
                } else {
                    giphyApi.search(query)
                }
                _uiState.update { it.copy(gifResults = response.data, isSearchingGifs = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearchingGifs = false, gifResults = emptyList()) }
            }
        }
    }
}
