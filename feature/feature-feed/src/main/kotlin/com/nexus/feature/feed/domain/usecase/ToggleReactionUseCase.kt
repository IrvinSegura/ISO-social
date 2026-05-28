package com.nexus.feature.feed.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.repository.FeedRepository
import javax.inject.Inject

class ToggleReactionUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(postId: String, emoji: String): NexusResult<Unit> =
        repository.toggleReaction(postId, emoji)
}
