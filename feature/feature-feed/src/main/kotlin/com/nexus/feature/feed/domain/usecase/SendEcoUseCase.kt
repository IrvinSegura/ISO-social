package com.nexus.feature.feed.domain.usecase

import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.repository.FeedRepository
import javax.inject.Inject

class SendEcoUseCase @Inject constructor(
    private val repository: FeedRepository,
) {
    suspend operator fun invoke(postId: String, message: String): NexusResult<Unit> {
        if (message.isBlank()) {
            return NexusResult.Error(NexusException.UnknownException(IllegalArgumentException("El eco no puede estar vacío.")))
        }
        return repository.sendEco(postId, message.trim())
    }
}
