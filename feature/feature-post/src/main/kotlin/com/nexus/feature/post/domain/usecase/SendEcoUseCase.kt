package com.nexus.feature.post.domain.usecase

import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class SendEcoUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(
        postId: String,
        type: EcoType,
        message: String = "",
        emoji: String? = null,
        mediaUrl: String? = null,
    ): NexusResult<Unit> {
        val valid = when (type) {
            EcoType.TEXT -> message.isNotBlank()
            EcoType.EMOJI -> emoji != null
            EcoType.GIF, EcoType.IMAGE, EcoType.AUDIO -> mediaUrl != null
        }
        if (!valid) {
            return NexusResult.Error(
                NexusException.UnknownException(IllegalArgumentException("El eco no puede estar vacío.")),
            )
        }
        return repository.sendEco(postId, type, message.trim(), emoji, mediaUrl)
    }
}
