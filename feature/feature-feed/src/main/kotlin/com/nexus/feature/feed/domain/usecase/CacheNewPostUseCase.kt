package com.nexus.feature.feed.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.repository.FeedRepository
import javax.inject.Inject

class CacheNewPostUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(postId: String): NexusResult<Unit> = repository.cachePost(postId)
}
