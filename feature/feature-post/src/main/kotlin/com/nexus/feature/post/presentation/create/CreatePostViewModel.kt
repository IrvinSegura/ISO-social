package com.nexus.feature.post.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val user = auth.currentUser
        _uiState.update {
            it.copy(
                currentUserName = user?.displayName.orEmpty(),
                currentUserPhotoUrl = user?.photoUrl?.toString(),
            )
        }
    }

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            is CreatePostIntent.ContentChanged -> {
                _uiState.update {
                    it.copy(content = intent.value.take(MAX_CONTENT_LENGTH), contentError = null)
                }
            }
            is CreatePostIntent.ImageSelected -> {
                _uiState.update { it.copy(selectedImageUri = intent.uri) }
            }
            CreatePostIntent.RemoveImage -> {
                _uiState.update { it.copy(selectedImageUri = null) }
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
        val imageUri = _uiState.value.selectedImageUri
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    publishingStep = if (imageUri != null) PublishingStep.UploadingImage else PublishingStep.SavingPost,
                    contentError = null,
                )
            }
            when (val result = createPostUseCase(content, circleId = null, circleName = null, imageUri = imageUri)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(publishingStep = PublishingStep.Idle) }
                    _events.send(CreatePostEvent.PostCreated)
                }
                is NexusResult.Error -> {
                    _uiState.update {
                        it.copy(publishingStep = PublishingStep.Idle, error = result.exception.message)
                    }
                }
            }
        }
    }
}
