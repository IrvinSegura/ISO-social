package com.nexus.feature.feed.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.nexus.feature.feed.data.mapper.toDomain
import com.nexus.feature.feed.data.remote.StoryRemoteDataSource
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import com.nexus.feature.feed.domain.repository.StoryMe
import com.nexus.feature.feed.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class StoryRepositoryImpl @Inject constructor(
    private val remote: StoryRemoteDataSource,
    private val auth: FirebaseAuth,
) : StoryRepository {

    override fun currentUser(): StoryMe? {
        val user = auth.currentUser ?: return null
        return StoryMe(
            uid = user.uid,
            name = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')
                ?: "Tú",
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override fun getActiveStories(): Flow<List<Story>> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val seenIds = if (currentUserId.isBlank()) {
            flowOf(emptySet())
        } else {
            // Starts empty so stories show up without waiting for the seen list
            remote.getSeenStoryIds(currentUserId).onStart { emit(emptySet()) }
        }
        return combine(remote.getActiveStories(), seenIds) { dtos, seen ->
            dtos.map { dto ->
                // Las propias historias se marcan como vistas para diferenciarse visualmente
                dto.toDomain(seenByCurrentUser = dto.authorId == currentUserId || dto.id in seen)
            }
        }
    }

    override suspend fun createStory(
        type: StoryType,
        mediaLocalUri: String?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String?,
        musicStartMs: Long,
        musicDurationMs: Long,
    ): Result<Story> = try {
        // Not runCatching: it would also swallow coroutine cancellation
        Result.success(
            createStoryOrThrow(
                type, mediaLocalUri, text, gradient, musicTitle, musicArtist,
                musicPreviewUrl, musicStartMs, musicDurationMs,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun createStoryOrThrow(
        type: StoryType,
        mediaLocalUri: String?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String?,
        musicStartMs: Long,
        musicDurationMs: Long,
    ): Story {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        val uri = mediaLocalUri?.let { Uri.parse(it) }
        return remote.uploadMediaAndCreateStory(
            type = type,
            mediaUri = uri,
            text = text,
            gradient = gradient,
            musicTitle = musicTitle,
            musicArtist = musicArtist,
            musicPreviewUrl = musicPreviewUrl,
            musicStartMs = musicStartMs,
            musicDurationMs = musicDurationMs,
            authorId = user.uid,
            // Must equal users/{uid}.displayName (firestore.rules); the viewer shows a fallback
            authorName = user.displayName.orEmpty(),
            authorAvatarUrl = user.photoUrl?.toString(),
        ).toDomain()
    }

    override suspend fun markSeen(storyId: String) {
        val userId = auth.currentUser?.uid ?: return
        remote.markSeen(storyId, userId)
    }
}
