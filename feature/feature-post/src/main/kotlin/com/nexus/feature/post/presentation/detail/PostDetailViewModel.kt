package com.nexus.feature.post.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nexus.core.common.result.NexusResult
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
) : ViewModel() {

    private val postId: String = savedStateHandle.toRoute<PostDetailRoute>().postId

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadPost()
    }

    fun onIntent(intent: PostDetailIntent) {
        when (intent) {
            PostDetailIntent.OpenEcoSheet -> _uiState.update { it.copy(ecoSheetOpen = true) }
            PostDetailIntent.CloseEcoSheet -> _uiState.update { it.copy(ecoSheetOpen = false) }
            is PostDetailIntent.SendEco -> sendEco(intent.message)
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

    private fun sendEco(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEcoSending = true) }
            when (val result = sendEcoUseCase(postId, message)) {
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
}
