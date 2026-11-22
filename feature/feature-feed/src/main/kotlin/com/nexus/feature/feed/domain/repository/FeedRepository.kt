package com.nexus.feature.feed.domain.repository

import androidx.paging.PagingData
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeed(): Flow<PagingData<Post>>
    suspend fun sendEco(postId: String, message: String): NexusResult<Unit>
    suspend fun toggleReaction(postId: String, emoji: String): NexusResult<Unit>
    /** Fetches a single post (e.g. one just published) into the local feed cache. */
    suspend fun cachePost(postId: String): NexusResult<Unit>
    /** Copies the server's eco count into the cache (e.g. after an eco sent from the detail). */
    suspend fun syncEcoCount(postId: String): NexusResult<Unit>

    /** Refreshes the cached counters of [postId] after the user reacted somewhere else. */
    suspend fun syncReaction(postId: String, myReaction: String?): NexusResult<Unit>
    /** Drops a post the author deleted from the cached feed. */
    suspend fun removeFromCache(postId: String)
    /** Deletes every cached post and paging key (e.g. on sign-out). */
    suspend fun clearCache()
}
