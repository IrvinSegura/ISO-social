package com.nexus.feature.post.domain.repository

import android.net.Uri
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail

interface PostRepository {
    suspend fun createPost(
        content: String,
        circleId: String?,
        circleName: String?,
        imageUris: List<Uri>,
        audioPath: String?,
        fileUri: Uri?,
        fileName: String?,
        mood: String?,
        locationName: String?,
    ): NexusResult<String>
    suspend fun getPostDetail(postId: String): NexusResult<PostDetail>
    suspend fun sendEco(
        postId: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
    ): NexusResult<Unit>
}
