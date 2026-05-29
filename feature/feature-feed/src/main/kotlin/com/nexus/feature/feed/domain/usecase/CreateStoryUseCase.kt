package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import com.nexus.feature.feed.domain.repository.StoryRepository
import javax.inject.Inject

class CreateStoryUseCase @Inject constructor(
    private val repository: StoryRepository,
) {
    suspend operator fun invoke(
        type: StoryType,
        mediaLocalUri: String?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String? = null,
        musicStartMs: Long = 0L,
        musicDurationMs: Long = 30_000L,
    ): Result<Story> = repository.createStory(type, mediaLocalUri, text, gradient, musicTitle, musicArtist, musicPreviewUrl, musicStartMs, musicDurationMs)
}
