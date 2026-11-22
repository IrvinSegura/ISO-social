package com.nexus.feature.feed.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.PostChange
import com.nexus.core.firebase.post.PostActions
import com.nexus.core.firebase.post.ReportReason
import com.nexus.core.firebase.post.ReportResult
import com.nexus.feature.feed.domain.usecase.RemoveCachedPostUseCase
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.model.Post
import com.nexus.feature.feed.domain.usecase.CacheNewPostUseCase
import com.nexus.feature.feed.domain.usecase.GetFeedUseCase
import com.nexus.feature.feed.domain.usecase.SendEcoUseCase
import com.nexus.feature.feed.domain.usecase.SyncEcoCountUseCase
import com.nexus.feature.feed.domain.usecase.SyncReactionUseCase
import com.nexus.feature.feed.domain.usecase.ToggleReactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class FeedViewModel @Inject constructor(
    getFeedUseCase: GetFeedUseCase,
    private val sendEcoUseCase: SendEcoUseCase,
    private val toggleReactionUseCase: ToggleReactionUseCase,
    private val cacheNewPostUseCase: CacheNewPostUseCase,
    private val syncEcoCountUseCase: SyncEcoCountUseCase,
    private val syncReactionUseCase: SyncReactionUseCase,
    private val removeCachedPostUseCase: RemoveCachedPostUseCase,
    private val postActions: PostActions,
    private val newPostEventBus: NewPostEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())

    // Temp ids of posts being published in a circle (see observeNewPosts)
    private val circleTempIds = mutableSetOf<String>()
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val posts: Flow<PagingData<Post>> = getFeedUseCase().cachedIn(viewModelScope)

    // Firestore applies local writes to its listeners at once, so a tap on "Guardar" shows
    // up here instantly without a separate optimistic state
    val savedPostIds: StateFlow<Set<String>> = postActions.observeSavedIds()
        .catch { e -> Timber.w(e, "Could not observe saved posts") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Replaceable in tests to simulate the passage of time
    @VisibleForTesting
    internal var clock: () -> Long = System::currentTimeMillis

    // The first collection of the pager already refreshes, so the feed starts fresh
    private var lastRefreshAt = clock()

    init {
        observeNewPosts()
        observeEcosSentElsewhere()
        observePostChanges()
    }

    // A post was edited, deleted or reacted to from its detail screen: update the cached copy
    private fun observePostChanges() {
        viewModelScope.launch {
            newPostEventBus.postChanges.collect { change ->
                launch {
                    when (change) {
                        is PostChange.Deleted -> removeCachedPostUseCase(change.postId)
                        is PostChange.Edited -> cacheNewPostUseCase(change.postId)
                        is PostChange.Reacted -> syncReactionUseCase(change.postId, change.myReaction)
                    }
                }
            }
        }
    }

    // An eco sent from the post detail does not go through this ViewModel, so the
    // cached count is refreshed from the server when the detail reports it
    private fun observeEcosSentElsewhere() {
        viewModelScope.launch {
            newPostEventBus.ecoSent.collect { postId -> launch { syncEcoCountUseCase(postId) } }
        }
    }

    private fun observeNewPosts() {
        viewModelScope.launch {
            newPostEventBus.events.collect { payload ->
                when {
                    // A circle post belongs to its circle, never to the public feed. Its status
                    // updates only carry the temp id, so it is remembered to ignore them too
                    payload.isPending && payload.circleId != null -> circleTempIds += payload.tempId
                    payload.tempId in circleTempIds -> {
                        if (payload.confirmedId != null) circleTempIds -= payload.tempId
                    }
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
                    // Confirmado por servidor → traer el post real a la caché y quitar el pendiente
                    else -> {
                        val confirmedId = payload.confirmedId ?: return@collect
                        launch {
                            val cached = cacheNewPostUseCase(confirmedId)
                            removePendingPost(payload.tempId)
                            // The post exists on the server; if it could not be cached, a refresh
                            // brings it in instead of leaving it invisible until the next one
                            if (cached is NexusResult.Error) requestRefresh()
                        }
                    }
                }
            }
        }
    }

    private fun removePendingPost(tempId: String) {
        _uiState.update { state ->
            state.copy(pendingPosts = state.pendingPosts.filter { it.tempId != tempId })
        }
    }

    private fun retryPendingPost(tempId: String) {
        _uiState.update { state ->
            state.copy(
                pendingPosts = state.pendingPosts.map { p ->
                    if (p.tempId == tempId) p.copy(status = PendingPostStatus.UPLOADING) else p
                },
            )
        }
        viewModelScope.launch { newPostEventBus.requestRetry(tempId) }
    }

    private fun dismissPendingPost(tempId: String) {
        removePendingPost(tempId)
        viewModelScope.launch { newPostEventBus.requestDiscard(tempId) }
    }

    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.OpenEcoSheet -> _uiState.update { it.copy(ecoTargetPostId = intent.postId) }
            FeedIntent.CloseEcoSheet -> _uiState.update { it.copy(ecoTargetPostId = null) }
            is FeedIntent.SendEco -> sendEco(intent.postId, intent.message)
            is FeedIntent.ToggleReaction -> toggleReaction(intent.postId, intent.emoji)
            is FeedIntent.RetryPendingPost -> retryPendingPost(intent.tempId)
            is FeedIntent.DismissPendingPost -> dismissPendingPost(intent.tempId)
            is FeedIntent.NavigateToPost -> viewModelScope.launch {
                _events.send(FeedEvent.NavigateToPost(intent.postId))
            }
            FeedIntent.NavigateToCreatePost -> viewModelScope.launch {
                _events.send(FeedEvent.NavigateToCreatePost)
            }
            FeedIntent.DismissError -> _uiState.update { it.copy(error = null) }
            is FeedIntent.ToggleSave -> toggleSave(intent.postId)
            is FeedIntent.ReportPost -> reportPost(intent.postId, intent.reason, intent.details)
            FeedIntent.ScreenResumed -> if (isFeedStale()) requestRefresh()
            FeedIntent.Refresh -> requestRefresh()
        }
    }

    // Coming back from a post or from background used to reload the whole feed every
    // time. New posts are already inserted into the cache, so a reload is only worth it
    // once the data is old, and never while a post is still uploading
    private fun isFeedStale(): Boolean {
        val isUploading = _uiState.value.pendingPosts.any { it.status == PendingPostStatus.UPLOADING }
        return !isUploading && clock() - lastRefreshAt >= STALE_AFTER_MS
    }

    private fun requestRefresh() {
        lastRefreshAt = clock()
        viewModelScope.launch { _events.send(FeedEvent.RefreshFeed) }
    }

    // El contador de ecos y las reacciones se actualizan de forma optimista en Room
    // (repositorio), así que aquí solo queda reflejar el resultado en la UI.
    private fun sendEco(postId: String, message: String) {
        _uiState.update { it.copy(isEcoSending = true) }
        viewModelScope.launch {
            when (val result = sendEcoUseCase(postId, message)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isEcoSending = false, ecoTargetPostId = null) }
                    _events.send(FeedEvent.EcoSent)
                }
                is NexusResult.Error -> _uiState.update {
                    it.copy(isEcoSending = false, error = result.exception.message)
                }
            }
        }
    }

    private fun toggleSave(postId: String) {
        val save = postId !in savedPostIds.value
        viewModelScope.launch {
            try {
                postActions.setSaved(postId, save)
                _events.send(FeedEvent.ShowMessage(if (save) "Guardado en tu perfil" else "Quitado de guardados"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo guardar la publicación") }
            }
        }
    }

    private fun reportPost(postId: String, reason: ReportReason, details: String?) {
        viewModelScope.launch {
            try {
                val message = when (postActions.report(postId, reason, details)) {
                    ReportResult.SENT -> "Gracias, revisaremos esta publicación"
                    ReportResult.ALREADY_REPORTED -> "Ya habías reportado esta publicación"
                }
                _events.send(FeedEvent.ShowMessage(message))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo enviar el reporte") }
            }
        }
    }

    companion object {
        @VisibleForTesting
        internal const val STALE_AFTER_MS = 5 * 60 * 1000L
    }

    private fun toggleReaction(postId: String, emoji: String) {
        viewModelScope.launch {
            val result = toggleReactionUseCase(postId, emoji)
            if (result is NexusResult.Error) {
                _uiState.update { it.copy(error = result.exception.message) }
            }
        }
    }
}
