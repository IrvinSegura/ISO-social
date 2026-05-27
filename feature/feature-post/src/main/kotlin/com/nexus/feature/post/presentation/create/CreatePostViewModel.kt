package com.nexus.feature.post.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.usecase.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_CONTENT_LENGTH = 500
private const val MIN_CONTENT_LENGTH = 10

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            is CreatePostIntent.ContentChanged -> {
                val trimmed = intent.value.take(MAX_CONTENT_LENGTH)
                _uiState.update {
                    it.copy(content = trimmed, contentError = null)
                }
            }
            CreatePostIntent.Publish -> publish()
            CreatePostIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun publish() {
        val content = _uiState.value.content.trim()
        if (content.length < MIN_CONTENT_LENGTH) {
            _uiState.update {
                it.copy(contentError = "Añade un poco más de contexto (mínimo $MIN_CONTENT_LENGTH caracteres).")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, contentError = null) }
            when (val result = createPostUseCase(content, circleId = null, circleName = null)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isPosting = false) }
                    _events.send(CreatePostEvent.PostCreated)
                }
                is NexusResult.Error -> {
                    _uiState.update {
                        it.copy(isPosting = false, error = result.exception.message)
                    }
                }
            }
        }
    }
}
