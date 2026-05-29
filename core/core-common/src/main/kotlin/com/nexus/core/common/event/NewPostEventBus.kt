package com.nexus.core.common.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPostEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NewPostPayload>(extraBufferCapacity = 8)
    val events: SharedFlow<NewPostPayload> = _events.asSharedFlow()

    suspend fun emitPending(payload: NewPostPayload) = _events.emit(payload)
    suspend fun emitConfirmed(tempId: String, confirmedId: String) =
        _events.emit(NewPostPayload(
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
        ))
    suspend fun emitFailed(tempId: String) =
        _events.emit(NewPostPayload(
            tempId = tempId,
            authorId = "",
            authorName = "",
            authorPhotoUrl = null,
            content = "",
            mood = null,
            locationName = null,
            createdAt = 0,
            isPending = false,
            confirmedId = null,
        ))
}
