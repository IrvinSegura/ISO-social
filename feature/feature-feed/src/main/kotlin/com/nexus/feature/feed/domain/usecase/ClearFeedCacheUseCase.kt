package com.nexus.feature.feed.domain.usecase

import com.nexus.feature.feed.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Wipes the local feed cache. Called when the session ends, so the next user never
 * sees the previous user's reactions ("myReaction") or cached posts.
 */
class ClearFeedCacheUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke() = repository.clearCache()
}
