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
    ): String
    suspend fun sendEco(
        postId: String,
        authorId: String,
        authorName: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
    )
}
