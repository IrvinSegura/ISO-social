package com.nexus.feature.post.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.nexus.core.common.result.NexusResult
import com.nexus.core.common.result.safeCall
import com.nexus.feature.post.data.mapper.toDomain
import com.nexus.feature.post.data.mapper.toDetail
import com.nexus.feature.post.data.remote.PostRemoteDataSource
import com.nexus.feature.post.domain.model.PostDetail
import com.nexus.feature.post.domain.repository.PostRepository
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val remoteDataSource: PostRemoteDataSource,
    private val auth: FirebaseAuth,
) : PostRepository {

    override suspend fun createPost(
        content: String,
        circleId: String?,
        circleName: String?,
        imageUri: Uri?,
    ): NexusResult<String> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.createPost(
            authorId = user.uid,
            authorName = user.displayName.orEmpty(),
            authorPhotoUrl = user.photoUrl?.toString(),
            content = content,
            circleId = circleId,
            circleName = circleName,
            imageUri = imageUri,
        )
    }

    override suspend fun getPostDetail(postId: String): NexusResult<PostDetail> = safeCall {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val postDto = remoteDataSource.getPost(postId)
        val ecos = remoteDataSource.getEcos(postId).map { it.toDomain() }
        postDto.toDetail(currentUserId, ecos)
    }

    override suspend fun sendEco(postId: String, message: String): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.sendEco(
            postId = postId,
            authorId = user.uid,
            authorName = user.displayName.orEmpty(),
            message = message,
        )
    }
}
