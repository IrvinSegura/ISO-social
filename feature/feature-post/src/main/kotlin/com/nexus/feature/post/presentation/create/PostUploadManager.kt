package com.nexus.feature.post.presentation.create

import android.net.Uri
import com.nexus.core.common.dispatchers.ApplicationScope
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.PendingPostRequest
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.usecase.CreatePostUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class PostDraft(
    val content: String,
    val imageUris: List<Uri>,
    val audioPath: String?,
    val fileUri: Uri?,
    val fileName: String?,
    val mood: String?,
    val locationName: String?,
    // A circle post: only its members see it. The name must match the circle's (rules check it)
    val circleId: String? = null,
    val circleName: String? = null,
    // A GIF or animated emoji from Giphy: nothing to upload, only its URL is stored
    val gifUrl: String? = null,
)

/**
 * Uploads posts outside the ViewModel lifecycle: CreatePostScreen closes right after
 * "Publicar", which used to cancel the upload together with viewModelScope.
 * Failed drafts are kept in memory so the feed can retry them.
 *
 * Note: work only survives while the process is alive; WorkManager would be needed
 * to survive process death.
 */
@Singleton
class PostUploadManager @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val newPostEventBus: NewPostEventBus,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val failedDrafts = ConcurrentHashMap<String, PostDraft>()

    init {
        appScope.launch {
            newPostEventBus.requests.collect { request ->
                when (request) {
                    is PendingPostRequest.Retry ->
                        failedDrafts.remove(request.tempId)?.let { upload(request.tempId, it) }
                    is PendingPostRequest.Discard ->
                        failedDrafts.remove(request.tempId)?.let(::deleteTempFiles)
                }
            }
        }
    }

    fun enqueue(tempId: String, draft: PostDraft) {
        appScope.launch { upload(tempId, draft) }
    }

    private suspend fun upload(tempId: String, draft: PostDraft) {
        val result = createPostUseCase(
            content = draft.content,
            circleId = draft.circleId,
            circleName = draft.circleName,
            imageUris = draft.imageUris,
            audioPath = draft.audioPath,
            fileUri = draft.fileUri,
            fileName = draft.fileName,
            mood = draft.mood,
            locationName = draft.locationName,
            gifUrl = draft.gifUrl,
        )
        when (result) {
            is NexusResult.Success -> {
                deleteTempFiles(draft)
                newPostEventBus.emitConfirmed(tempId, result.data)
            }
            is NexusResult.Error -> {
                Timber.e(result.exception, "Post upload failed")
                failedDrafts[tempId] = draft
                newPostEventBus.emitFailed(tempId)
            }
        }
    }

    // The recorded audio lives in cacheDir and is useless once uploaded or discarded
    private fun deleteTempFiles(draft: PostDraft) {
        draft.audioPath?.let { File(it).delete() }
    }
}
