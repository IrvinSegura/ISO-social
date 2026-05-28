package com.nexus.feature.feed.data.remote

import com.nexus.feature.feed.data.model.PostDto

interface FeedRemoteDataSource {
    suspend fun getPosts(afterTimestamp: Long?, pageSize: Int): List<PostDto>
    suspend fun sendEco(postId: String, authorId: String, authorName: String, message: String)
    suspend fun toggleReaction(postId: String, userId: String, emoji: String)
}
