package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.repository.FeedRepository
import javax.inject.Inject

class RemoveCachedPostUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(postId: String) = repository.removeFromCache(postId)
}
