package com.nexus.feature.feed.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexus.feature.feed.data.model.PostDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FeedRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FeedRemoteDataSource {

    override suspend fun getPosts(afterTimestamp: Long?, pageSize: Int): List<PostDto> {
        var query = firestore.collection(POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (afterTimestamp != null) {
            query = query.startAfter(afterTimestamp)
        }

        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(PostDto::class.java)?.copy(id = doc.id)
        }
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
        firestore.collection(POSTS).document(postId)
            .collection(ECOS).add(eco).await()

        firestore.collection(POSTS).document(postId)
            .update("ecoCount", FieldValue.increment(1)).await()
    }

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
    }
}
