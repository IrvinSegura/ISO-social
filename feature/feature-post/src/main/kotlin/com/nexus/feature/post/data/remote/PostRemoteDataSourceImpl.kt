package com.nexus.feature.post.data.remote

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import com.nexus.feature.post.domain.model.EcoType
import kotlinx.coroutines.tasks.await
import java.io.File
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
        imageUris: List<Uri>,
        audioPath: String?,
        fileUri: Uri?,
        fileName: String?,
        mood: String?,
        locationName: String?,
    ): String {
        val postId = firestore.collection(POSTS).document().id

        // Upload images
        val imageUrls = imageUris.mapIndexed { index, uri ->
            val ref = storage.reference.child("posts/$postId/images/image_$index.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

        // Upload audio
        val audioUrl: String? = audioPath?.let { path ->
            val file = File(path)
            val ref = storage.reference.child("posts/$postId/audio.m4a")
            ref.putFile(Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        }

        // Upload file
        val uploadedFileUrl: String? = fileUri?.let { uri ->
            val safeName = fileName ?: "file"
            val ref = storage.reference.child("posts/$postId/files/$safeName")
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
            "imageUrls" to imageUrls,
            "audioUrl" to audioUrl,
            "fileUrl" to uploadedFileUrl,
            "fileName" to fileName,
            "mood" to mood,
            "locationName" to locationName,
            "ecoCount" to 0,
            "reactions" to emptyMap<String, Long>(),
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
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
    ) {
        val ecoData = mutableMapOf<String, Any?>(
            "authorId" to authorId,
            "authorName" to authorName,
            "type" to type.name,
            "message" to message,
            "emoji" to emoji,
            "mediaUrl" to mediaUrl,
            "createdAt" to System.currentTimeMillis(),
        )
        firestore.collection(POSTS).document(postId)
            .collection(ECOS).add(ecoData).await()
        firestore.collection(POSTS).document(postId)
            .update("ecoCount", FieldValue.increment(1)).await()
    }

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
    }
}
