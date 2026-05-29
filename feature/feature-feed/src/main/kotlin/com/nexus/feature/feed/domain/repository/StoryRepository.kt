package com.nexus.feature.feed.domain.repository

import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getActiveStories(): Flow<List<Story>>
    suspend fun createStory(
        type: StoryType,
        mediaLocalUri: String?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String? = null,
        musicStartMs: Long = 0L,
        musicDurationMs: Long = 30_000L,
    ): Result<Story>
    suspend fun markSeen(storyId: String)
}
