package com.nexus.feature.feed.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.nexus.feature.feed.data.mapper.toDomain
import com.nexus.feature.feed.data.remote.StoryRemoteDataSource
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import com.nexus.feature.feed.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val remote: StoryRemoteDataSource,
    private val auth: FirebaseAuth,
) : StoryRepository {

    override fun getActiveStories(): Flow<List<Story>> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        return remote.getActiveStories().map { dtos ->
            dtos.map { dto ->
                // Las propias historias se marcan como vistas para diferenciarse visualmente
                dto.toDomain(seenByCurrentUser = dto.authorId == currentUserId)
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
    ): Result<Story> = runCatching {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        val uri = mediaLocalUri?.let { Uri.parse(it) }
        remote.uploadMediaAndCreateStory(
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
            authorName = user.displayName ?: "Anónimo",
        ).toDomain()
    }

    override suspend fun markSeen(storyId: String) {
        val userId = auth.currentUser?.uid ?: return
        remote.markSeen(storyId, userId)
    }
}
