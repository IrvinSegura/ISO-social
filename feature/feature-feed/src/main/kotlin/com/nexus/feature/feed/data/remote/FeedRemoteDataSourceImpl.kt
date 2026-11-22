package com.nexus.feature.feed.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nexus.core.firebase.post.PostActions
import com.nexus.feature.feed.data.model.PostDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FeedRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val postActions: PostActions,
) : FeedRemoteDataSource {

    override suspend fun getPosts(after: PostCursor?, pageSize: Int): List<PostDto> {
        // Ordering by id as well makes the cursor stable when two posts share a timestamp.
        // Only public posts: circle posts live in their circle, and the rules reject a
        // query that could return them (index: circleId, createdAt, __name__)
        var query = firestore.collection(POSTS)
            .whereEqualTo("circleId", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (after != null) {
            query = query.startAfter(after.createdAt, after.postId)
        }

        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(PostDto::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun getPost(postId: String): PostDto? {
        val doc = firestore.collection(POSTS).document(postId).get().await()
        return doc.toObject(PostDto::class.java)?.copy(id = doc.id)
    }

    override suspend fun getUserReactions(
        postIds: List<String>,
        userId: String,
    ): Map<String, String> = coroutineScope {
        // One query per chunk on the user's own mirror (users/{uid}/reactions/{postId})
        // instead of one get() per post; only posts with a reaction are read and billed
        val myReactions = firestore.collection(USERS).document(userId).collection(MY_REACTIONS)
        postIds.distinct().chunked(MAX_IN_QUERY_VALUES).map { chunk ->
            async {
                myReactions.whereIn(FieldPath.documentId(), chunk).get().await()
                    .documents.mapNotNull { doc -> doc.getString("emoji")?.let { doc.id to it } }
            }
        }.awaitAll().flatten().toMap()
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
            "type" to "TEXT",
            "createdAt" to System.currentTimeMillis(),
        )
        val postRef = firestore.collection(POSTS).document(postId)
        val ecoRef = postRef.collection(ECOS).document()
        val batch = firestore.batch()
        batch.set(ecoRef, eco)
        // set+merge creates ecoCount if missing; lastEcoId lets the security rules check
        // that the +1 belongs to the eco written in this same batch
        batch.set(
            postRef,
            mapOf("ecoCount" to FieldValue.increment(1), "lastEcoId" to ecoRef.id),
            SetOptions.merge(),
        )
        batch.commit().await()
    }

    // Shared with the post detail, which reacts through the same transaction
    override suspend fun toggleReaction(postId: String, userId: String, emoji: String): String? =
        postActions.toggleReaction(postId, emoji)

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
        private const val USERS = "users"

        // Private copy of the user's reactions, keyed by post id
        private const val MY_REACTIONS = "reactions"

        // Firestore limit for the values of a whereIn filter
        private const val MAX_IN_QUERY_VALUES = 30
    }
}
