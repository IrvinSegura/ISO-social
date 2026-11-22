package com.nexus.feature.post.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class SetEcoLikedUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(postId: String, ecoId: String, liked: Boolean): NexusResult<Unit> =
        repository.setEcoLiked(postId, ecoId, liked)
}
