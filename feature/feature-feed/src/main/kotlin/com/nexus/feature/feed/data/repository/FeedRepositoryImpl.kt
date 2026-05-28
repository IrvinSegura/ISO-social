package com.nexus.feature.feed.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.auth.FirebaseAuth
import com.nexus.core.common.result.NexusResult
import com.nexus.core.common.result.safeCall
import com.nexus.feature.feed.data.local.db.FeedDatabase
import com.nexus.feature.feed.data.mapper.toDomain
import com.nexus.feature.feed.data.remote.FeedRemoteDataSource
import com.nexus.feature.feed.data.remote.FeedRemoteMediator
import com.nexus.feature.feed.domain.model.Post
import com.nexus.feature.feed.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class FeedRepositoryImpl @Inject constructor(
    private val remoteDataSource: FeedRemoteDataSource,
    private val db: FeedDatabase,
    private val auth: FirebaseAuth,
) : FeedRepository {

    override fun getFeed(): Flow<PagingData<Post>> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            remoteMediator = FeedRemoteMediator(remoteDataSource, db),
            pagingSourceFactory = { db.postDao().pagingSource() },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain(currentUserId) }
        }
    }

    override suspend fun sendEco(postId: String, message: String): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.sendEco(
            postId = postId,
            authorId = user.uid,
            authorName = user.displayName.orEmpty(),
            message = message,
        )
    }

    override suspend fun toggleReaction(postId: String, emoji: String): NexusResult<Unit> = safeCall {
        val user = auth.currentUser ?: error("Usuario no autenticado")
        remoteDataSource.toggleReaction(postId, user.uid, emoji)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
