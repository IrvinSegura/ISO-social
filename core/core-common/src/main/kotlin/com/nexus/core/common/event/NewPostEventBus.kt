package com.nexus.core.common.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the feed asks the uploader to do with a pending post. */
sealed interface PendingPostRequest {
    val tempId: String

    data class Retry(override val tempId: String) : PendingPostRequest
    data class Discard(override val tempId: String) : PendingPostRequest
}

/** A post changed outside the screen that shows it. */
sealed interface PostChange {
    val postId: String

    data class Edited(override val postId: String) : PostChange
    data class Deleted(override val postId: String) : PostChange

    /** The user reacted from the post detail; [myReaction] is their reaction now, or null. */
    data class Reacted(override val postId: String, val myReaction: String?) : PostChange
}

@Singleton
class NewPostEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NewPostPayload>(extraBufferCapacity = 8)
    val events: SharedFlow<NewPostPayload> = _events.asSharedFlow()

    // Feed â uploader channel, so the feed module does not depend on the post module
    private val _requests = MutableSharedFlow<PendingPostRequest>(extraBufferCapacity = 8)
    val requests: SharedFlow<PendingPostRequest> = _requests.asSharedFlow()

    // Post detail â feed: an eco was sent elsewhere, so the cached eco count is stale
    private val _ecoSent = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val ecoSent: SharedFlow<String> = _ecoSent.asSharedFlow()

    suspend fun emitEcoSent(postId: String) = _ecoSent.emit(postId)

    // Post detail → feed and profile: the author edited or deleted a post, so cached copies are stale
    private val _postChanges = MutableSharedFlow<PostChange>(extraBufferCapacity = 8)
    val postChanges: SharedFlow<PostChange> = _postChanges.asSharedFlow()

    suspend fun emitPostChange(change: PostChange) = _postChanges.emit(change)

    suspend fun emitPending(payload: NewPostPayload) = _events.emit(payload)

    suspend fun emitConfirmed(tempId: String, confirmedId: String) =
        _events.emit(statusPayload(tempId, confirmedId))

    suspend fun emitFailed(tempId: String) =
        _events.emit(statusPayload(tempId, confirmedId = null))

    suspend fun requestRetry(tempId: String) = _requests.emit(PendingPostRequest.Retry(tempId))

    suspend fun requestDiscard(tempId: String) = _requests.emit(PendingPostRequest.Discard(tempId))

    private fun statusPayload(tempId: String, confirmedId: String?) = NewPostPayload(
        tempId = tempId,
        authorId = "",
        authorName = "",
        authorPhotoUrl = null,
        content = "",
        mood = null,
        locationName = null,
        createdAt = 0,
        isPending = false,
        confirmedId = confirmedId,
    )
}
