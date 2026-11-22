package com.nexus.feature.feed.data

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.nexus.feature.feed.data.local.dao.PostDao
import com.nexus.feature.feed.data.local.dao.RemoteKeyDao
import com.nexus.feature.feed.data.local.db.FeedDatabase
import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.local.entity.PostReactionRow
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity
import com.nexus.feature.feed.data.model.PostDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot

/** In-memory PostDao: enough of Room's behavior for repository and mediator tests. */
class FakePostDao : PostDao {
    val rows = linkedMapOf<String, PostEntity>()

    override fun pagingSource(): PagingSource<Int, PostEntity> = error("Not used in unit tests")

    override suspend fun getById(postId: String): PostEntity? = rows[postId]

    override suspend fun getMyReactions(postIds: List<String>): List<PostReactionRow> =
        postIds.mapNotNull { id -> rows[id]?.takeIf { it.myReaction != null } }
            .map { PostReactionRow(it.id, it.myReaction) }

    override suspend fun insertAll(posts: List<PostEntity>) {
        posts.forEach { rows[it.id] = it }
    }

    override suspend fun updateReactions(postId: String, reactions: Map<String, Int>, myReaction: String?) {
        rows[postId]?.let { rows[postId] = it.copy(reactions = reactions, myReaction = myReaction) }
    }

    override suspend fun addToEcoCount(postId: String, delta: Int) {
        rows[postId]?.let { rows[postId] = it.copy(ecoCount = maxOf(0, it.ecoCount + delta)) }
    }

    override suspend fun setEcoCount(postId: String, count: Int) {
        rows[postId]?.let { rows[postId] = it.copy(ecoCount = count) }
    }

    override suspend fun deleteById(postId: String) {
        rows.remove(postId)
    }

    override suspend fun clearAll() = rows.clear()
}

class FakeRemoteKeyDao : RemoteKeyDao {
    val keys = linkedMapOf<String, RemoteKeyEntity>()

    override suspend fun insertAll(keys: List<RemoteKeyEntity>) {
        keys.forEach { this.keys[it.postId] = it }
    }

    override suspend fun remoteKeyForPost(postId: String): RemoteKeyEntity? = keys[postId]

    override suspend fun clearAll() = keys.clear()
}

/** A FeedDatabase whose withTransaction simply runs the block on the fake DAOs. */
fun fakeFeedDatabase(postDao: PostDao, remoteKeyDao: RemoteKeyDao): FeedDatabase {
    val db = mockk<FeedDatabase>()
    every { db.postDao() } returns postDao
    every { db.remoteKeyDao() } returns remoteKeyDao
    mockkStatic("androidx.room.RoomDatabaseKt")
    val block = slot<suspend () -> Any?>()
    coEvery { db.withTransaction(capture(block)) } coAnswers { block.captured.invoke() }
    return db
}

fun postEntity(
    id: String,
    reactions: Map<String, Int> = emptyMap(),
    myReaction: String? = null,
    createdAt: Long = 1_000L,
) = PostEntity(
    id = id,
    authorId = "author",
    authorName = "Author",
    authorPhotoUrl = null,
    content = "content $id",
    circleId = null,
    circleName = null,
    ecoCount = 0,
    reactions = reactions,
    myReaction = myReaction,
    createdAt = createdAt,
    updatedAt = createdAt,
)

fun postDto(id: String, createdAt: Long, reactions: Map<String, Any> = emptyMap()) = PostDto(
    id = id,
    authorId = "author",
    authorName = "Author",
    content = "content $id",
    reactions = reactions,
    createdAt = createdAt,
    updatedAt = createdAt,
)
