package com.nexus.feature.post.domain.usecase

import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

/** Saves the author's edit of a post's text and mood (the same limits as when creating it). */
class UpdatePostUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(postId: String, content: String, mood: String?): NexusResult<Unit> {
        val text = content.trim()
        if (text.isEmpty() || text.length > MAX_CONTENT_LENGTH) {
            return NexusResult.Error(
                NexusException.UnknownException(IllegalArgumentException("El texto debe tener entre 1 y 500 caracteres.")),
            )
        }
        return repository.updatePost(postId, text, mood?.takeIf { it.isNotBlank() })
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
