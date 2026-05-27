package com.nexus.feature.feed.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.nexus.feature.feed.data.local.db.FeedDatabase
import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity
import com.nexus.feature.feed.data.mapper.toEntity

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val remoteDataSource: FeedRemoteDataSource,
    private val db: FeedDatabase,
) : RemoteMediator<Int, PostEntity>() {

    private val postDao = db.postDao()
    private val remoteKeyDao = db.remoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>,
    ): MediatorResult {
        return try {
            val afterTimestamp: Long? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    remoteKeyDao.remoteKeyForPost(lastItem.id)?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val posts = remoteDataSource.getPosts(
                afterTimestamp = afterTimestamp,
                pageSize = state.config.pageSize,
            )
            val endReached = posts.size < state.config.pageSize

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.clearAll()
                    postDao.clearAll()
                }
                val keys = posts.map { post ->
                    RemoteKeyEntity(
                        postId = post.id,
                        prevKey = null,
                        nextKey = if (endReached) null else post.createdAt,
                    )
                }
                remoteKeyDao.insertAll(keys)
                postDao.insertAll(posts.map { it.toEntity() })
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
