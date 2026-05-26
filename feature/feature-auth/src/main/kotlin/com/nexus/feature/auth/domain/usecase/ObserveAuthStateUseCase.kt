package com.nexus.feature.auth.domain.usecase

import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<User?> = repository.authStateFlow
}
