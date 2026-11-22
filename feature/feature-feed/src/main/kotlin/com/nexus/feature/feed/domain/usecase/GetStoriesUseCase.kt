package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.repository.StoryMe
import com.nexus.feature.feed.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStoriesUseCase @Inject constructor(
    private val repository: StoryRepository,
) {
    operator fun invoke(): Flow<List<Story>> = repository.getActiveStories()

    /** Who is signed in, so the row can show their own avatar first. */
    fun me(): StoryMe? = repository.currentUser()
}
