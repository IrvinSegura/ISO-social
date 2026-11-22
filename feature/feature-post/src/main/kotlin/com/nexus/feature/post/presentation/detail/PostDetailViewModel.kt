package com.nexus.feature.post.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.PostChange
import com.nexus.core.firebase.post.PostActions
import com.nexus.core.firebase.post.ReportReason
import com.nexus.core.firebase.post.ReportResult
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.replyTarget
import com.nexus.feature.post.domain.usecase.DeletePostUseCase
import com.nexus.feature.post.domain.usecase.GetPostDetailUseCase
import com.nexus.feature.post.domain.usecase.SendEcoUseCase
import com.nexus.feature.post.domain.usecase.SetEcoLikedUseCase
import com.nexus.feature.post.domain.usecase.TogglePostReactionUseCase
import com.nexus.feature.post.domain.usecase.UpdatePostUseCase
import com.nexus.feature.post.presentation.navigation.PostDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val sendEcoUseCase: SendEcoUseCase,
    private val newPostEventBus: NewPostEventBus,
    private val postActions: PostActions,
    private val updatePostUseCase: UpdatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val setEcoLikedUseCase: SetEcoLikedUseCase,
    private val toggleReactionUseCase: TogglePostReactionUseCase,
) : ViewModel() {

    private val postId: String = savedStateHandle.toRoute<PostDetailRoute>().postId

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadPost()
        observeSaved()
    }

    fun onIntent(intent: PostDetailIntent) {
        when (intent) {
            is PostDetailIntent.SendTextEco -> sendEco(EcoType.TEXT, intent.message, null, null)
            is PostDetailIntent.SendEmojiEco -> sendEco(EcoType.EMOJI, "", intent.emoji, null)
            is PostDetailIntent.SendMediaEco -> sendEco(intent.type, "", null, intent.mediaUrl)
            is PostDetailIntent.ReplyTo -> _uiState.update { it.copy(replyingTo = intent.eco.replyTarget()) }
            PostDetailIntent.CancelReply -> _uiState.update { it.copy(replyingTo = null) }
            is PostDetailIntent.ToggleEcoLike -> toggleEcoLike(intent.ecoId)
            is PostDetailIntent.React -> react(intent.emoji)
            PostDetailIntent.DismissError -> _uiState.update { it.copy(error = null) }
            PostDetailIntent.Retry -> loadPost()
            PostDetailIntent.ToggleSave -> toggleSave()
            is PostDetailIntent.Report -> report(intent.reason, intent.details)
            is PostDetailIntent.SaveEdit -> saveEdit(intent.content, intent.mood)
            PostDetailIntent.Delete -> delete()
        }
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, loadFailed = false) }
            when (val result = getPostDetailUseCase(postId)) {
                is NexusResult.Success -> _uiState.update {
                    it.copy(isLoading = false, post = result.data)
                }
                is NexusResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message, loadFailed = true)
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
        if (_uiState.value.isEcoSending) return
        val replyTo = _uiState.value.replyingTo
        viewModelScope.launch {
            _uiState.update { it.copy(isEcoSending = true) }
            when (val result = sendEcoUseCase(postId, type, message, emoji, mediaUrl, replyTo)) {
                is NexusResult.Success -> {
                    _uiState.update { it.copy(isEcoSending = false, replyingTo = null) }
                    _events.send(PostDetailEvent.EcoSent)
                    // Lets the feed update this post's cached eco count
                    newPostEventBus.emitEcoSent(postId)
                    loadPost()
                }
                is NexusResult.Error -> _uiState.update {
                    it.copy(isEcoSending = false, error = result.exception.message)
                }
            }
        }
    }

    // Optimistic: the heart changes at once and goes back if the write fails
    private fun toggleEcoLike(ecoId: String) {
        val eco = _uiState.value.post?.ecos?.firstOrNull { it.id == ecoId } ?: return
        val liked = !eco.likedByMe
        updateEco(ecoId) { it.copy(likedByMe = liked, likeCount = (it.likeCount + if (liked) 1 else -1).coerceAtLeast(0)) }
        viewModelScope.launch {
            val result = setEcoLikedUseCase(postId, ecoId, liked)
            if (result is NexusResult.Error) {
                Timber.w(result.exception, "Could not change the heart on an eco")
                updateEco(ecoId) { it.copy(likedByMe = eco.likedByMe, likeCount = eco.likeCount) }
                _events.send(PostDetailEvent.ShowMessage("No se pudo guardar tu corazón. Revisa tu conexión."))
            }
        }
    }

    private fun updateEco(ecoId: String, change: (Eco) -> Eco) = _uiState.update { state ->
        state.copy(post = state.post?.copy(ecos = state.post.ecos.map { if (it.id == ecoId) change(it) else it }))
    }

    private var reacting = false

    // Optimistic like the feed; taps while one is in flight are ignored so they cannot interleave
    private fun react(emoji: String) {
        val post = _uiState.value.post ?: return
        if (reacting) return
        reacting = true
        val before = post.myReaction
        val guess = if (before == emoji) null else emoji
        setReaction(from = before, to = guess)
        viewModelScope.launch {
            when (val result = toggleReactionUseCase(postId, emoji)) {
                is NexusResult.Success -> {
                    // The server decides from its own state; follow it if it disagrees
                    if (result.data != guess) setReaction(from = guess, to = result.data)
                    newPostEventBus.emitPostChange(PostChange.Reacted(postId, result.data))
                }
                is NexusResult.Error -> {
                    Timber.w(result.exception, "Could not react to the post")
                    setReaction(from = guess, to = before)
                    _events.send(PostDetailEvent.ShowMessage("No se pudo reaccionar. Revisa tu conexión."))
                }
            }
            reacting = false
        }
    }

    private fun setReaction(from: String?, to: String?) = _uiState.update { state ->
        val post = state.post ?: return@update state
        state.copy(post = post.copy(reactions = post.reactions.moveReaction(from, to), myReaction = to))
    }

    private fun observeSaved() {
        viewModelScope.launch {
            postActions.observeSavedIds()
                .map { postId in it }
                .distinctUntilChanged()
                .catch { Timber.w(it, "Saved posts listener failed") }
                .collect { saved -> _uiState.update { it.copy(isSaved = saved) } }
        }
    }

    private fun toggleSave() {
        val save = !_uiState.value.isSaved
        viewModelScope.launch {
            try {
                postActions.setSaved(postId, save)
                _events.send(PostDetailEvent.ShowMessage(if (save) "Guardado en tu perfil" else "Quitado de guardados"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not change the saved state")
                _events.send(PostDetailEvent.ShowMessage("No se pudo guardar. Revisa tu conexión."))
            }
        }
    }

    private fun report(reason: ReportReason, details: String?) {
        viewModelScope.launch {
            val message = try {
                when (postActions.report(postId, reason, details)) {
                    ReportResult.SENT -> "Gracias, revisaremos esta publicación"
                    ReportResult.ALREADY_REPORTED -> "Ya habías reportado esta publicación"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not report the post")
                "No se pudo enviar el reporte. Inténtalo de nuevo."
            }
            _events.send(PostDetailEvent.ShowMessage(message))
        }
    }

    private fun saveEdit(content: String, mood: String?) {
        if (_uiState.value.isSavingEdit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEdit = true) }
            when (val result = updatePostUseCase(postId, content, mood)) {
                is NexusResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSavingEdit = false,
                            post = state.post?.copy(content = content.trim(), mood = mood?.takeIf { it.isNotBlank() }),
                        )
                    }
                    // The feed and the profile refresh their copies
                    newPostEventBus.emitPostChange(PostChange.Edited(postId))
                    _events.send(PostDetailEvent.EditSaved)
                    _events.send(PostDetailEvent.ShowMessage("Publicación actualizada"))
                }
                is NexusResult.Error -> {
                    Timber.w(result.exception, "Could not edit the post")
                    _uiState.update { it.copy(isSavingEdit = false) }
                    val text = (result.exception.cause as? IllegalArgumentException)?.message
                        ?: "No se pudieron guardar los cambios. Revisa tu conexión."
                    _events.send(PostDetailEvent.ShowMessage(text))
                }
            }
        }
    }

    private fun delete() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = deletePostUseCase(postId)) {
                is NexusResult.Success -> {
                    if (_uiState.value.isSaved) runCatching { postActions.setSaved(postId, false) }
                    newPostEventBus.emitPostChange(PostChange.Deleted(postId))
                    _events.send(PostDetailEvent.Deleted)
                }
                is NexusResult.Error -> {
                    Timber.w(result.exception, "Could not delete the post")
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(PostDetailEvent.ShowMessage("No se pudo eliminar. Revisa tu conexión."))
                }
            }
        }
    }
}

/** Moves one reaction from [from] to [to] (either may be null); counts never go below zero. */
internal fun Map<String, Int>.moveReaction(from: String?, to: String?): Map<String, Int> {
    if (from == to) return this
    val counts = toMutableMap()
    from?.let { counts[it] = ((counts[it] ?: 0) - 1).coerceAtLeast(0) }
    to?.let { counts[it] = (counts[it] ?: 0) + 1 }
    return counts.filterValues { it > 0 }
}
