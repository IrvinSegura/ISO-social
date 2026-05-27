package com.nexus.feature.post.data.remote

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
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
        imageUri: Uri?,
    ): String {
        // Generate the ID client-side so we can name the Storage path before uploading
        val postId = firestore.collection(POSTS).document().id

        val imageUrl: String? = imageUri?.let { uri ->
            val ref = storage.reference.child("posts/$postId/image.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

        val now = System.currentTimeMillis()
        val data = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoUrl" to authorPhotoUrl,
            "content" to content,
            "circleId" to circleId,
            "circleName" to circleName,
            "imageUrl" to imageUrl,
            "ecoCount" to 0,
            "createdAt" to now,
            "updatedAt" to now,
        )
        firestore.collection(POSTS).document(postId).set(data).await()
        return postId
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
