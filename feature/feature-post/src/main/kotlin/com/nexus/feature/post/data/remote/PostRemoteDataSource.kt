package com.nexus.feature.post.data.remote

import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto

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
    ): String
    suspend fun sendEco(postId: String, authorId: String, authorName: String, message: String)
}
