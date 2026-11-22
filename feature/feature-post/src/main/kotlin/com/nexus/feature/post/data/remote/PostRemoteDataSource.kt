package com.nexus.feature.post.data.remote

import android.net.Uri
import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import com.nexus.feature.post.domain.model.EcoType

interface PostRemoteDataSource {
    suspend fun getPost(postId: String): PostDto
    suspend fun getEcos(postId: String): List<EcoDto>
    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String?,
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
    ): String
    suspend fun sendEco(
        postId: String,
        authorId: String,
        authorName: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
        parentId: String?,
        replyToName: String?,
    )
    /** Gives or takes back [userId]'s heart on an eco. */
    suspend fun setEcoLiked(postId: String, ecoId: String, userId: String, liked: Boolean)
    /** Changes what the author may edit: the text and the mood. */
    suspend fun updatePost(postId: String, content: String, mood: String?)
    /** Deletes the post, then (best effort) the media in [authorId]'s folder for it. */
    suspend fun deletePost(postId: String, authorId: String)
}
