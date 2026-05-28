package com.nexus.feature.feed.domain.repository

import androidx.paging.PagingData
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getFeed(): Flow<PagingData<Post>>
    suspend fun sendEco(postId: String, message: String): NexusResult<Unit>
    suspend fun toggleReaction(postId: String, emoji: String): NexusResult<Unit>
}
