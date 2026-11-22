package com.nexus.feature.post.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.nexus.core.common.result.NexusResult
import com.nexus.core.common.result.safeCall
import com.nexus.feature.post.data.mapper.toDomain
import com.nexus.feature.post.data.mapper.toDetail
import com.nexus.feature.post.data.remote.PostRemoteDataSource
import com.nexus.core.firebase.post.PostActions
import com.nexus.feature.post.domain.model.EcoReplyTarget
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail
import com.nexus.feature.post.domain.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val remoteDataSource: PostRemoteDataSource,
    private val auth: FirebaseAuth,
    private val postActions: PostActions,
) : PostRepository {

    override suspend fun createPost(
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
    ): NexusResult<String> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.createPost(
            authorId = user.uid,
            authorName = user.displayName.orEmpty(),
            authorPhotoUrl = user.photoUrl?.toString(),
            content = content,
            circleId = circleId,
            circleName = circleName,
            imageUris = imageUris,
            audioPath = audioPath,
            fileUri = fileUri,
            fileName = fileName,
            mood = mood,
            locationName = locationName,
            gifUrl = gifUrl,
        )
    }

    override suspend fun getPostDetail(postId: String): NexusResult<PostDetail> = safeCall {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        // Independent reads: in parallel, the screen waits for the slower one instead of both
        coroutineScope {
            val post = async { remoteDataSource.getPost(postId) }
            val ecos = async { remoteDataSource.getEcos(postId) }
            // A missing reaction must not hide the post, so its failure only loses the highlight
            val reaction = async { runCatching { postActions.myReaction(postId) }.getOrNull() }
            post.await().toDetail(
                currentUserId = currentUserId,
                ecos = ecos.await().map { it.toDomain(currentUserId) },
                myReaction = reaction.await(),
            )
        }
    }

    override suspend fun sendEco(
        postId: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
        replyTo: EcoReplyTarget?,
    ): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.sendEco(
            postId = postId,
            authorId = user.uid,
            authorName = user.displayName.orEmpty(),
            type = type,
            message = message,
            emoji = emoji,
            mediaUrl = mediaUrl,
            parentId = replyTo?.parentId,
            replyToName = replyTo?.name,
        )
    }

    override suspend fun setEcoLiked(postId: String, ecoId: String, liked: Boolean): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.setEcoLiked(postId, ecoId, user.uid, liked)
    }

    override suspend fun toggleReaction(postId: String, emoji: String): NexusResult<String?> = safeCall {
        postActions.toggleReaction(postId, emoji)
    }

    override suspend fun updatePost(postId: String, content: String, mood: String?): NexusResult<Unit> = safeCall {
        remoteDataSource.updatePost(postId, content, mood)
    }

    override suspend fun deletePost(postId: String): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.deletePost(postId, user.uid)
    }
}
