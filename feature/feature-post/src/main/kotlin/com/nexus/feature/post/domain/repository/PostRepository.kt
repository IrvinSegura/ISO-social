package com.nexus.feature.post.domain.repository

import android.net.Uri
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.model.EcoReplyTarget
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
        gifUrl: String?,
    ): NexusResult<String>
    suspend fun getPostDetail(postId: String): NexusResult<PostDetail>
    suspend fun sendEco(
        postId: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
        replyTo: EcoReplyTarget? = null,
    ): NexusResult<Unit>
    suspend fun setEcoLiked(postId: String, ecoId: String, liked: Boolean): NexusResult<Unit>
    /** Returns the user's reaction after the change, as the server decided it. */
    suspend fun toggleReaction(postId: String, emoji: String): NexusResult<String?>
    suspend fun updatePost(postId: String, content: String, mood: String?): NexusResult<Unit>
    suspend fun deletePost(postId: String): NexusResult<Unit>
}
