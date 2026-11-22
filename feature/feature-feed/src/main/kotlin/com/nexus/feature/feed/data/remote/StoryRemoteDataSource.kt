package com.nexus.feature.feed.data.remote

import android.net.Uri
import com.nexus.feature.feed.data.model.StoryDto
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.flow.Flow

interface StoryRemoteDataSource {
    fun getActiveStories(): Flow<List<StoryDto>>
    /** Ids of the stories the user has already seen (only recent ones, stories last 24h). */
    fun getSeenStoryIds(userId: String): Flow<Set<String>>
    suspend fun uploadMediaAndCreateStory(
        type: StoryType,
        mediaUri: Uri?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String? = null,
        musicStartMs: Long = 0L,
        musicDurationMs: Long = 30_000L,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String?,
    ): StoryDto
    suspend fun markSeen(storyId: String, userId: String)
}
