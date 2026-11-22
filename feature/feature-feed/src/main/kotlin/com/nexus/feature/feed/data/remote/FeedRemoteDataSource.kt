package com.nexus.feature.feed.data.remote

import com.nexus.feature.feed.data.model.PostDto

/** Position after which the next page starts; the id breaks ties between equal timestamps. */
data class PostCursor(val createdAt: Long, val postId: String)

interface FeedRemoteDataSource {
    suspend fun getPosts(after: PostCursor?, pageSize: Int): List<PostDto>
    suspend fun getPost(postId: String): PostDto?
    /** postId → emoji the user reacted with, only for posts where there is a reaction. */
    suspend fun getUserReactions(postIds: List<String>, userId: String): Map<String, String>
    suspend fun sendEco(postId: String, authorId: String, authorName: String, message: String)
    /**
     * Toggles the reaction on the server and returns the user's reaction right after it
     * (null = none). Counters are not returned: reading them would put the post document
     * in the transaction and make concurrent reactions contend on it.
     */
    suspend fun toggleReaction(postId: String, userId: String, emoji: String): String?
}
