package com.nexus.feature.post.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PostRemoteDataSource {

    override suspend fun getPost(postId: String): PostDto {
        val doc = firestore.collection(POSTS).document(postId).get().await()
        return doc.toObject(PostDto::class.java)?.copy(id = doc.id)
            ?: error("Post $postId not found")
    }

    override suspend fun getEcos(postId: String): List<EcoDto> {
        return firestore.collection(POSTS).document(postId)
            .collection(ECOS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(EcoDto::class.java)?.copy(id = doc.id)
            }
    }

    override suspend fun createPost(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String?,
        content: String,
        circleId: String?,
        circleName: String?,
    ): String {
        val now = System.currentTimeMillis()
        val data = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "circleId" to circleId,
            "circleName" to circleName,
            "ecoCount" to 0,
            "createdAt" to now,
            "updatedAt" to now,
        )
        return firestore.collection(POSTS).add(data).await().id
    }

    override suspend fun sendEco(
        postId: String,
        authorId: String,
        authorName: String,
        message: String,
    ) {
        val eco = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "message" to message,
            "createdAt" to System.currentTimeMillis(),
        )
        firestore.collection(POSTS).document(postId).collection(ECOS).add(eco).await()
        firestore.collection(POSTS).document(postId)
            .update("ecoCount", FieldValue.increment(1)).await()
    }

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
    }
}
