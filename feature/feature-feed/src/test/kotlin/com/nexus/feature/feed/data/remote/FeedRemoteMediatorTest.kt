package com.nexus.feature.feed.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.nexus.feature.feed.data.FakePostDao
import com.nexus.feature.feed.data.FakeRemoteKeyDao
import com.nexus.feature.feed.data.fakeFeedDatabase
import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.local.entity.RemoteKeyEntity
import com.nexus.feature.feed.data.postDto
import com.nexus.feature.feed.data.postEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediatorTest {

    private val postDao = FakePostDao()
    private val remoteKeyDao = FakeRemoteKeyDao()
    private val remote = mockk<FeedRemoteDataSource>()
    private lateinit var mediator: FeedRemoteMediator

    @Before
    fun setup() {
        mediator = FeedRemoteMediator(remote, fakeFeedDatabase(postDao, remoteKeyDao), USER_ID)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `refresh replaces the cache and marks the user's reactions`() = runTest {
        postDao.rows["stale"] = postEntity("stale")
        coEvery { remote.getPosts(null, PAGE_SIZE) } returns listOf(postDto("a", 20L), postDto("b", 10L))
        coEvery { remote.getUserReactions(listOf("a", "b"), USER_ID) } returns mapOf("b" to "🔥")

        val result = mediator.load(LoadType.REFRESH, state())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf("a", "b"), postDao.rows.keys.toList())
        assertEquals(null, postDao.rows.getValue("a").myReaction)
        assertEquals("🔥", postDao.rows.getValue("b").myReaction)
    }

    @Test
    fun `refresh keeps cached reactions when they cannot be loaded`() = runTest {
        postDao.rows["a"] = postEntity("a", myReaction = "❤️")
        coEvery { remote.getPosts(null, PAGE_SIZE) } returns listOf(postDto("a", 20L))
        coEvery { remote.getUserReactions(any(), any()) } throws RuntimeException("offline")

        val result = mediator.load(LoadType.REFRESH, state())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals("❤️", postDao.rows.getValue("a").myReaction)
    }

    @Test
    fun `a full page is not the end and stores keys for the next one`() = runTest {
        val page = (1..PAGE_SIZE).map { postDto("p$it", createdAt = 100L - it) }
        coEvery { remote.getPosts(null, PAGE_SIZE) } returns page
        coEvery { remote.getUserReactions(any(), any()) } returns emptyMap()

        val result = mediator.load(LoadType.REFRESH, state())

        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(page.last().createdAt, remoteKeyDao.keys.getValue(page.last().id).nextKey)
    }

    @Test
    fun `append continues after the last loaded post`() = runTest {
        val last = postEntity("last", createdAt = 50L)
        remoteKeyDao.keys["last"] = RemoteKeyEntity("last", prevKey = null, nextKey = 50L)
        coEvery { remote.getPosts(PostCursor(50L, "last"), PAGE_SIZE) } returns listOf(postDto("older", 40L))
        coEvery { remote.getUserReactions(any(), any()) } returns emptyMap()

        val result = mediator.load(LoadType.APPEND, state(loaded = listOf(last)))

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        coVerify { remote.getPosts(PostCursor(50L, "last"), PAGE_SIZE) }
        assertTrue("older" in postDao.rows)
    }

    @Test
    fun `append stops when the last page reached the end`() = runTest {
        val last = postEntity("last", createdAt = 50L)
        remoteKeyDao.keys["last"] = RemoteKeyEntity("last", prevKey = null, nextKey = null)

        val result = mediator.load(LoadType.APPEND, state(loaded = listOf(last)))

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        coVerify(exactly = 0) { remote.getPosts(any(), any()) }
    }

    @Test
    fun `a server error is reported to paging`() = runTest {
        coEvery { remote.getPosts(any(), any()) } throws RuntimeException("offline")

        val result = mediator.load(LoadType.REFRESH, state())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    private fun state(loaded: List<PostEntity> = emptyList()): PagingState<Int, PostEntity> {
        val pages: List<PagingSource.LoadResult.Page<Int, PostEntity>> = if (loaded.isEmpty()) {
            emptyList()
        } else {
            listOf(PagingSource.LoadResult.Page(data = loaded, prevKey = null, nextKey = null))
        }
        return PagingState(
            pages = pages,
            anchorPosition = null,
            config = PagingConfig(pageSize = PAGE_SIZE),
            leadingPlaceholderCount = 0,
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val USER_ID = "user-1"
    }
}
