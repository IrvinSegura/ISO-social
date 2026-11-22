package com.nexus.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.feature.feed.domain.usecase.CreateStoryUseCase
import com.nexus.feature.feed.domain.usecase.GetStoriesUseCase
import com.nexus.feature.feed.domain.usecase.MarkStorySeenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val getStoriesUseCase: GetStoriesUseCase,
    private val createStoryUseCase: CreateStoryUseCase,
    private val markStorySeenUseCase: MarkStorySeenUseCase,
) : ViewModel() {

    // Everything except the stories themselves, which come from the listener below
    private val _uiState = MutableStateFlow(StoryUiState(me = getStoriesUseCase.me()))

    // Stories marked as seen on this device before Firestore confirms it
    private val locallySeen = MutableStateFlow(emptySet<String>())

    private val remoteStories = getStoriesUseCase()
        // A dead listener closes the flow; re-subscribe a few times with backoff
        // (transient network errors) before giving up and showing the error
        .retryWhen { cause, attempt ->
            if (cause is CancellationException || attempt >= MAX_STORY_RETRIES) {
                false
            } else {
                delay(STORY_RETRY_BASE_MS shl attempt.toInt())
                true
            }
        }
        .catch { e ->
            _uiState.update { it.copy(error = e.message) }
            // combine only emits once every source has a value: without this, a listener
            // that fails before its first snapshot would never show the error.
            // A later failure keeps the stories already on screen
            if (uiState.value.stories.isEmpty()) emit(emptyList())
        }

    // WhileSubscribed: the Firestore listeners stop 5 s after the feed leaves the screen
    // (app in background), instead of billing reads for as long as the ViewModel lives.
    // The last stories stay in the state, so coming back shows no empty flash
    val uiState: StateFlow<StoryUiState> =
        combine(_uiState, remoteStories, locallySeen) { state, stories, seen ->
            state.copy(
                stories = stories.map { s ->
                    if (s.id in seen && !s.hasBeenSeen) s.copy(hasBeenSeen = true) else s
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = StoryUiState(),
        )

    private val _events = Channel<StoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
        val story = uiState.value.stories.firstOrNull { it.id == storyId }
        if (story == null || story.hasBeenSeen) return

        // Optimista: se marca al instante; el listener de Firestore lo confirma después
        locallySeen.update { it + storyId }
        viewModelScope.launch {
            try {
                markStorySeenUseCase(storyId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "No se pudo marcar la historia como vista")
            }
        }
    }

    private companion object {
        const val MAX_STORY_RETRIES = 3L
        const val STORY_RETRY_BASE_MS = 2_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
