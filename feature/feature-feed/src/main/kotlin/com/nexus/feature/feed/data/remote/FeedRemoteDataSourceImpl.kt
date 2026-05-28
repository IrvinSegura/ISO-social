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
            "type" to "TEXT",
            "createdAt" to System.currentTimeMillis(),
        )
        firestore.collection(POSTS).document(postId)
            .collection(ECOS).add(eco).await()

        firestore.collection(POSTS).document(postId)
            .update("ecoCount", FieldValue.increment(1)).await()
    }

    override suspend fun toggleReaction(postId: String, userId: String, emoji: String) {
        val postRef = firestore.collection(POSTS).document(postId)
        val userReactionRef = postRef.collection(USER_REACTIONS).document(userId)

        firestore.runTransaction { tx ->
            val userReactionSnap = tx.get(userReactionRef)
            val currentEmoji = if (userReactionSnap.exists()) {
                userReactionSnap.getString("emoji")
            } else null

            when {
                currentEmoji == emoji -> {
                    // Same emoji → remove reaction
                    tx.delete(userReactionRef)
                    tx.update(postRef, "reactions.$emoji", FieldValue.increment(-1L))
                }
                currentEmoji != null -> {
                    // Different emoji → swap
                    tx.update(postRef, "reactions.$currentEmoji", FieldValue.increment(-1L))
                    tx.set(userReactionRef, mapOf("emoji" to emoji))
                    tx.update(postRef, "reactions.$emoji", FieldValue.increment(1L))
                }
                else -> {
                    // No reaction → add
                    tx.set(userReactionRef, mapOf("emoji" to emoji))
                    tx.update(postRef, "reactions.$emoji", FieldValue.increment(1L))
                }
            }
        }.await()
    }

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
        private const val USER_REACTIONS = "userReactions"
    }
}
