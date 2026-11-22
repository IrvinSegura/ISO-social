package com.nexus.feature.post.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

/** Reacts with an emoji, or takes the reaction back; returns the reaction the server kept. */
class TogglePostReactionUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(postId: String, emoji: String): NexusResult<String?> =
        repository.toggleReaction(postId, emoji)
}
