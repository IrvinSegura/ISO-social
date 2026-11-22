package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.repository.StoryRepository
import javax.inject.Inject

class MarkStorySeenUseCase @Inject constructor(
    private val repository: StoryRepository,
) {
    suspend operator fun invoke(storyId: String) = repository.markSeen(storyId)
}
