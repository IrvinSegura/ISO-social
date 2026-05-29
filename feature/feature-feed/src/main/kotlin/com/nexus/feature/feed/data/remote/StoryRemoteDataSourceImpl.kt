package com.nexus.feature.feed.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.nexus.feature.feed.data.model.StoryDto
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class StoryRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : StoryRemoteDataSource {

    override fun getActiveStories(): Flow<List<StoryDto>> = callbackFlow {
        val now = System.currentTimeMillis()
        // Un solo orderBy evita requerir índice compuesto en Firestore
        val listener = firestore.collection(STORIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error al escuchar stories")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val stories = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StoryDto::class.java)?.copy(id = doc.id)
                }.filter { it.expiresAt > now } // filtrar expiradas en cliente
                trySend(stories)
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
    ): StoryDto {
        val mediaUrl = if (mediaUri != null) {
            val ext = if (type == StoryType.VIDEO || type == StoryType.REEL) "mp4" else "jpg"
            val ref = storage.reference.child("stories/$authorId/${UUID.randomUUID()}.$ext")
            ref.putFile(mediaUri).await()
            ref.downloadUrl.await().toString()
        } else null

        val now = System.currentTimeMillis()
        val dto = StoryDto(
            authorId = authorId,
            authorName = authorName,
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
        )

        val ref = firestore.collection(STORIES).document()
        Timber.d("Guardando story en Firestore: authorId=${dto.authorId}, type=${dto.type}")
        ref.set(dto).await()
        Timber.d("Story guardada con id=${ref.id}")
        return dto.copy(id = ref.id)
    }

    override suspend fun markSeen(storyId: String, userId: String) {
        firestore.collection(STORIES).document(storyId)
            .collection(SEEN_BY).document(userId)
            .set(mapOf("seenAt" to System.currentTimeMillis())).await()
    }

    companion object {
        private const val STORIES = "stories"
        private const val SEEN_BY = "seenBy"
        private const val STORY_TTL_MS = 24 * 60 * 60 * 1000L // 24 horas
    }
}
