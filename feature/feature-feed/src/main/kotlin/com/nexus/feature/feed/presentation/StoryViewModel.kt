package com.nexus.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.feature.feed.domain.usecase.CreateStoryUseCase
import com.nexus.feature.feed.domain.usecase.GetStoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val getStoriesUseCase: GetStoriesUseCase,
    private val createStoryUseCase: CreateStoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _events = Channel<StoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeStories()
    }

    private fun observeStories() {
        viewModelScope.launch {
            getStoriesUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { stories -> _uiState.update { it.copy(stories = stories) } }
        }
    }

    fun onIntent(intent: StoryIntent) {
        when (intent) {
            is StoryIntent.CreateStory -> createStory(intent)
            is StoryIntent.MarkSeen -> markSeen(intent.storyId)
            StoryIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun createStory(intent: StoryIntent.CreateStory) {
        Timber.d("createStory: type=${intent.type}, text=${intent.text}")
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            createStoryUseCase(
                type = intent.type,
                mediaLocalUri = intent.mediaLocalUri,
                text = intent.text,
                gradient = intent.gradient,
                musicTitle = intent.musicTitle,
                musicArtist = intent.musicArtist,
                musicPreviewUrl = intent.musicPreviewUrl,
                musicStartMs = intent.musicStartMs,
                musicDurationMs = intent.musicDurationMs,
            ).onSuccess {
                Timber.d("Story creada exitosamente: ${it.id}")
                _uiState.update { s -> s.copy(isCreating = false) }
                _events.send(StoryEvent.StoryCreated)
            }.onFailure { e ->
                Timber.e(e, "Error al crear story")
                val msg = e.message ?: "Error desconocido al publicar"
                _uiState.update { s -> s.copy(isCreating = false, error = msg) }
                _events.send(StoryEvent.ShowError(msg))
            }
        }
    }

    private fun markSeen(storyId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    stories = state.stories.map { story ->
                        if (story.id == storyId) story.copy(hasBeenSeen = true) else story
                    },
                )
            }
        }
    }
}
