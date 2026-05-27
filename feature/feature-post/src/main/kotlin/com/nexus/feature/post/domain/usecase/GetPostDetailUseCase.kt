package com.nexus.feature.post.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.PostDetail
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class GetPostDetailUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(postId: String): NexusResult<PostDetail> =
        repository.getPostDetail(postId)
}
