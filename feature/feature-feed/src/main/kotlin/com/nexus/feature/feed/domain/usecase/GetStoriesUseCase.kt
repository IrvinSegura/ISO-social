package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStoriesUseCase @Inject constructor(
    private val repository: StoryRepository,
) {
    operator fun invoke(): Flow<List<Story>> = repository.getActiveStories()
}
