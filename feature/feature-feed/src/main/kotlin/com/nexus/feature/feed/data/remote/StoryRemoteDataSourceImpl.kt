package com.nexus.feature.feed.data.remote

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexus.feature.feed.data.model.StoryDto
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.nexus.core.common.media.ImageCompressor
import com.nexus.core.storage.MediaContent
import com.nexus.core.storage.MediaStorage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class StoryRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaStorage: MediaStorage,
    private val auth: FirebaseAuth,
    private val imageCompressor: ImageCompressor,
) : StoryRemoteDataSource {

    override fun getActiveStories(): Flow<List<StoryDto>> = callbackFlow {
        // Stories expire STORY_TTL_MS after creation, so filtering on createdAt keeps a
        // single-field query (no composite index) while skipping expired documents
        val since = System.currentTimeMillis() - STORY_TTL_MS
        val listener = firestore.collection(STORIES)
            .whereGreaterThan("createdAt", since)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // A failed listener is dead: close the flow so the error reaches the
                    // ViewModel (which retries) instead of leaving stale stories silently
                    Timber.e(error, "Error al escuchar stories")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                // "now" is evaluated on every snapshot so long-lived listeners also drop
                // stories that expired after subscribing
                val now = System.currentTimeMillis()
                val stories = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StoryDto::class.java)?.copy(id = doc.id)
                }.filter { it.expiresAt > now }
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    override fun getSeenStoryIds(userId: String): Flow<Set<String>> = callbackFlow {
        val since = System.currentTimeMillis() - STORY_TTL_MS
        val listener = firestore.collection(USERS).document(userId)
            .collection(SEEN_STORIES)
            .whereGreaterThan("seenAt", since)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error al escuchar historias vistas")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.map { it.id }.toSet())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun uploadMediaAndCreateStory(
        type: StoryType,
        mediaUri: Uri?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String?,
        musicStartMs: Long,
        musicDurationMs: Long,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String?,
    ): StoryDto {
        val isVideo = type == StoryType.VIDEO || type == StoryType.REEL
        // Inside the author's folder: the bucket policies only let a user write under their uid
        val mediaPath = mediaUri?.let {
            "stories/$authorId/${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
        }
        val mediaUrl = if (mediaUri != null && mediaPath != null) {
            // Photos go up compressed; videos, GIFs and undecodable files go up as is
            val compressed = if (!isVideo) imageCompressor.compress(mediaUri) else null
            val content = compressed?.let { MediaContent.Bytes(it, "image/jpeg") }
                ?: MediaContent.Local(mediaUri)
            mediaStorage.upload(mediaPath, content)
        } else null

        val now = System.currentTimeMillis()
        val dto = StoryDto(
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            type = type.name,
            mediaUrl = mediaUrl,
            text = text,
            backgroundGradient = gradient.name,
            musicTitle = musicTitle,
            musicArtist = musicArtist,
            musicPreviewUrl = musicPreviewUrl,
            musicStartMs = musicStartMs,
            musicDurationMs = musicDurationMs,
            createdAt = now,
            expiresAt = now + STORY_TTL_MS,
            expireAt = Timestamp(Date(now + STORY_TTL_MS)),
        )

        val ref = firestore.collection(STORIES).document()
        Timber.d("Guardando story en Firestore: authorId=${dto.authorId}, type=${dto.type}")
        try {
            ref.set(dto).await()
        } catch (e: Exception) {
            // Do not leave an orphaned file in storage when the story could not be saved
            if (mediaPath != null) {
                withContext(NonCancellable) { runCatching { mediaStorage.delete(mediaPath) } }
            }
            throw e
        }
        Timber.d("Story guardada con id=${ref.id}")
        return dto.copy(id = ref.id)
    }

    override suspend fun markSeen(storyId: String, userId: String) {
        val seenAt = mapOf("seenAt" to System.currentTimeMillis())
        // seenBy lets the author know who watched; users/{uid}/seenStories lets this user
        // know which stories were already watched with a single listener
        firestore.batch()
            .set(firestore.collection(STORIES).document(storyId).collection(SEEN_BY).document(userId), seenAt)
            .set(firestore.collection(USERS).document(userId).collection(SEEN_STORIES).document(storyId), seenAt)
            .commit()
            .await()
    }

    companion object {
        private const val STORIES = "stories"
        private const val SEEN_BY = "seenBy"
        private const val USERS = "users"
        private const val SEEN_STORIES = "seenStories"
        private const val STORY_TTL_MS = 24 * 60 * 60 * 1000L // 24 horas
    }
}
