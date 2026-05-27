package com.nexus.feature.post.domain.usecase

import android.net.Uri
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(
        content: String,
        circleId: String?,
        circleName: String?,
        imageUri: Uri?,
    ): NexusResult<String> = repository.createPost(content.trim(), circleId, circleName, imageUri)
}
