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
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val remoteDataSource: FeedRemoteDataSource,
    private val db: FeedDatabase,
    private val currentUserId: String,
) : RemoteMediator<Int, PostEntity>() {

    private val postDao = db.postDao()
    private val remoteKeyDao = db.remoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>,
    ): MediatorResult {
        return try {
            val cursor: PostCursor? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    // A null nextKey means the last page already reached the end
                    remoteKeyDao.remoteKeyForPost(lastItem.id)?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    PostCursor(createdAt = lastItem.createdAt, postId = lastItem.id)
                }
            }

            val posts = remoteDataSource.getPosts(
                after = cursor,
                pageSize = state.config.pageSize,
            )
            val endReached = posts.size < state.config.pageSize
            val postIds = posts.map { it.id }
            val fetchedReactions = loadMyReactions(postIds)

            db.withTransaction {
                // If the user's reactions could not be loaded, keep the ones already cached:
                // a wrong "no reaction" would make the next tap add instead of remove
                val myReactions = fetchedReactions
                    ?: postDao.getMyReactions(postIds).associate { it.id to it.myReaction }
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
                postDao.insertAll(posts.map { it.toEntity(myReaction = myReactions[it.id]) })
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    // A failure here must not fail the page; null means "unknown", not "no reactions"
    private suspend fun loadMyReactions(postIds: List<String>): Map<String, String>? {
        if (currentUserId.isBlank() || postIds.isEmpty()) return emptyMap()
        return try {
            remoteDataSource.getUserReactions(postIds, currentUserId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load user reactions")
            null
        }
    }
}
