package com.nexus.feature.feed.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.core.common.result.safeCall
import com.nexus.feature.feed.data.local.db.FeedDatabase
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity
import com.nexus.feature.feed.data.mapper.toDomain
import com.nexus.feature.feed.data.mapper.toEntity
import com.nexus.feature.feed.data.model.ReactionState
import com.nexus.feature.feed.data.remote.FeedRemoteDataSource
import com.nexus.feature.feed.data.remote.FeedRemoteMediator
import com.nexus.feature.feed.domain.model.Post
import com.nexus.feature.feed.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalPagingApi::class)
class FeedRepositoryImpl @Inject constructor(
    private val remoteDataSource: FeedRemoteDataSource,
    private val db: FeedDatabase,
    private val auth: FirebaseAuth,
) : FeedRepository {

    private val postDao = db.postDao()
    private val reactionLocks = ConcurrentHashMap<String, Mutex>()

    override fun getFeed(): Flow<PagingData<Post>> {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            remoteMediator = FeedRemoteMediator(remoteDataSource, db, currentUserId),
            pagingSourceFactory = { postDao.pagingSource() },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain(currentUserId) }
        }
    }

    // Room is the UI's source of truth: updating it first gives an instant (optimistic)
    // change that also survives refreshes; it is rolled back if the server rejects it.
    override suspend fun sendEco(postId: String, message: String): NexusResult<Unit> {
        // The count the row should have while our +1 is still the latest change to it
        val optimisticCount = runNonFatal("optimistic eco") {
            db.withTransaction {
                val row = postDao.getById(postId) ?: return@withTransaction null
                postDao.addToEcoCount(postId, 1)
                row.ecoCount + 1
            }
        }
        val result = safeCall {
            val user = auth.currentUser ?: error("Usuario no autenticado")
            remoteDataSource.sendEco(
                postId = postId,
                authorId = user.uid,
                authorName = user.displayName.orEmpty(),
                message = message,
            )
        }
        if (result is NexusResult.Error && optimisticCount != null) {
            // Undo only our +1: if a refresh already replaced the row with server data,
            // that data never had it, and a blind -1 would leave the count one too low
            runNonFatal("roll back eco") {
                db.withTransaction {
                    if (postDao.getById(postId)?.ecoCount == optimisticCount) {
                        postDao.addToEcoCount(postId, -1)
                    }
                }
            }
        }
        return result
    }

    override suspend fun removeFromCache(postId: String) {
        runNonFatal("remove deleted post") { postDao.deleteById(postId) }
    }

    override suspend fun clearCache() {
        runNonFatal("clear feed cache") {
            db.withTransaction {
                postDao.clearAll()
                db.remoteKeyDao().clearAll()
            }
        }
    }

    override suspend fun syncEcoCount(postId: String): NexusResult<Unit> = safeCall {
        val post = remoteDataSource.getPost(postId) ?: return@safeCall
        // The home feed is public only; a circle post is shown in its circle
        if (post.circleId != null) return@safeCall
        postDao.setEcoCount(post.id, post.ecoCount)
    }

    override suspend fun syncReaction(postId: String, myReaction: String?): NexusResult<Unit> = safeCall {
        val post = remoteDataSource.getPost(postId) ?: return@safeCall
        if (post.circleId != null) return@safeCall
        postDao.updateReactions(post.id, post.toEntity(myReaction).reactions, myReaction)
    }

    // Taps on the same post run one after another: each one starts from the state the
    // previous one left, and a rollback can never undo a later tap
    override suspend fun toggleReaction(postId: String, emoji: String): NexusResult<Unit> =
        reactionLocks.getOrPut(postId) { Mutex() }.withLock {
            val user = auth.currentUser ?: return@withLock NexusResult.Error(
                NexusException.UnknownException(IllegalStateException("User not authenticated")),
            )

            val before = runNonFatal("optimistic reaction") { applyOptimisticReaction(postId, emoji) }

            when (val result = safeCall { remoteDataSource.toggleReaction(postId, user.uid, emoji) }) {
                is NexusResult.Success -> {
                    // The server decides from its own state; if it disagrees with the guess
                    // (e.g. the user had already reacted on another device), follow it
                    runNonFatal("reconcile reaction") { reconcileReaction(postId, result.data) }
                    NexusResult.Success(Unit)
                }
                is NexusResult.Error -> {
                    before?.let { runNonFatal("roll back reaction") { rollBackReaction(postId, it, emoji) } }
                    result
                }
            }
        }

    /** Applies the tap to the cached post in one transaction; returns the state before it. */
    private suspend fun applyOptimisticReaction(postId: String, emoji: String): ReactionState? =
        db.withTransaction {
            val row = postDao.getById(postId) ?: return@withTransaction null
            val before = ReactionState(row.reactions, row.myReaction)
            val after = before.toggle(emoji)
            postDao.updateReactions(postId, after.reactions, after.myReaction)
            before
        }

    // Undoes only this tap's change on top of whatever the row holds now. If a refresh
    // already replaced the row with server data, there is nothing of ours left to undo
    private suspend fun rollBackReaction(postId: String, before: ReactionState, emoji: String) {
        val optimisticReaction = before.toggle(emoji).myReaction
        db.withTransaction {
            val row = postDao.getById(postId) ?: return@withTransaction
            if (row.myReaction != optimisticReaction) return@withTransaction
            val restored = ReactionState(row.reactions, row.myReaction).switchTo(before.myReaction)
            postDao.updateReactions(postId, restored.reactions, restored.myReaction)
        }
    }

    // Moves the cached reaction to the server's answer, adjusting the counters locally.
    // Exact counters come with the next refresh (the server no longer returns them).
    private suspend fun reconcileReaction(postId: String, serverReaction: String?) {
        db.withTransaction {
            val row = postDao.getById(postId) ?: return@withTransaction
            if (row.myReaction == serverReaction) return@withTransaction
            val fixed = ReactionState(row.reactions, row.myReaction).switchTo(serverReaction)
            postDao.updateReactions(postId, fixed.reactions, fixed.myReaction)
        }
    }

    // The local cache is cosmetic: a Room failure must not turn a saved reaction into an error
    private suspend fun <T> runNonFatal(action: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Could not $action")
        null
    }

    override suspend fun cachePost(postId: String): NexusResult<Unit> = safeCall {
        val post = remoteDataSource.getPost(postId) ?: return@safeCall
        // The home feed is public only; a circle post is shown in its circle
        if (post.circleId != null) return@safeCall
        db.withTransaction {
            postDao.insertAll(listOf(post.toEntity()))
            // A key lets paging append older posts even if this one ends up last
            db.remoteKeyDao().insertAll(
                listOf(RemoteKeyEntity(postId = post.id, prevKey = null, nextKey = post.createdAt)),
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
