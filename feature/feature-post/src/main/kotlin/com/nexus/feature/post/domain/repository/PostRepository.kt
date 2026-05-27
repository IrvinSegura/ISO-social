package com.nexus.feature.post.domain.repository

import android.net.Uri
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.PostDetail

interface PostRepository {
    suspend fun createPost(
        content: String,
        circleId: String?,
        circleName: String?,
        imageUri: Uri?,
    ): NexusResult<String>
    suspend fun getPostDetail(postId: String): NexusResult<PostDetail>
    suspend fun sendEco(postId: String, message: String): NexusResult<Unit>
}
