package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): NexusResult<Unit> = repository.signOut()
}
