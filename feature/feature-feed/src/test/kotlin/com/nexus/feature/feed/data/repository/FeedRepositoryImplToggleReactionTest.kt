package com.nexus.feature.feed.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.feed.data.FakePostDao
import com.nexus.feature.feed.data.FakeRemoteKeyDao
import com.nexus.feature.feed.data.fakeFeedDatabase
import com.nexus.feature.feed.data.postEntity
import com.nexus.feature.feed.data.remote.FeedRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedRepositoryImplToggleReactionTest {

    private val postDao = FakePostDao()
    private val remote = mockk<FeedRemoteDataSource>()
    private val auth = mockk<FirebaseAuth>()
    private lateinit var repository: FeedRepositoryImpl

    @Before
    fun setup() {
        val user = mockk<FirebaseUser> { every { uid } returns USER_ID }
        every { auth.currentUser } returns user
        repository = FeedRepositoryImpl(remote, fakeFeedDatabase(postDao, FakeRemoteKeyDao()), auth)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `success follows the server when it disagrees with the guess`() = runTest {
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 1))
        // The server knew better: the user had already reacted, so the tap removed it
        coEvery { remote.toggleReaction(POST, USER_ID, "❤️") } returns null

        val result = repository.toggleReaction(POST, "❤️")

        assertTrue(result is NexusResult.Success)
        // The optimistic +1 is undone locally; exact counters arrive with the next refresh
        assertEquals(mapOf("❤️" to 1), postDao.rows.getValue(POST).reactions)
        assertEquals(null, postDao.rows.getValue(POST).myReaction)
    }

    @Test
    fun `success keeps the optimistic state when the server agrees`() = runTest {
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 1))
        coEvery { remote.toggleReaction(POST, USER_ID, "🔥") } returns "🔥"

        repository.toggleReaction(POST, "🔥")

        assertEquals(mapOf("❤️" to 1, "🔥" to 1), postDao.rows.getValue(POST).reactions)
        assertEquals("🔥", postDao.rows.getValue(POST).myReaction)
    }

    @Test
    fun `the optimistic change is visible while the server works`() = runTest {
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 1))
        val server = CompletableDeferred<String?>()
        coEvery { remote.toggleReaction(POST, USER_ID, "🔥") } coAnswers { server.await() }

        val call = async { repository.toggleReaction(POST, "🔥") }
        runCurrent()

        assertEquals(mapOf("❤️" to 1, "🔥" to 1), postDao.rows.getValue(POST).reactions)
        assertEquals("🔥", postDao.rows.getValue(POST).myReaction)
        server.complete("🔥")
        call.await()
    }

    @Test
    fun `failure rolls the optimistic change back`() = runTest {
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 3, "🔥" to 1), myReaction = "❤️")
        coEvery { remote.toggleReaction(any(), any(), any()) } throws RuntimeException("offline")

        val result = repository.toggleReaction(POST, "🔥")

        assertTrue(result is NexusResult.Error)
        assertEquals(mapOf("❤️" to 3, "🔥" to 1), postDao.rows.getValue(POST).reactions)
        assertEquals("❤️", postDao.rows.getValue(POST).myReaction)
    }

    @Test
    fun `failure does not undo data a refresh wrote meanwhile`() = runTest {
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 1))
        val refreshed = postEntity(POST, reactions = mapOf("❤️" to 7))
        coEvery { remote.toggleReaction(any(), any(), any()) } coAnswers {
            // A feed refresh replaces the row with fresh server data before the call fails
            postDao.rows[POST] = refreshed
            throw RuntimeException("offline")
        }

        repository.toggleReaction(POST, "🔥")

        assertEquals(refreshed, postDao.rows.getValue(POST))
    }

    @Test
    fun `taps on the same post run one after another`() = runTest {
        postDao.rows[POST] = postEntity(POST)
        val firstServerCall = CompletableDeferred<String?>()
        coEvery { remote.toggleReaction(POST, USER_ID, "❤️") } coAnswers { firstServerCall.await() }
        coEvery { remote.toggleReaction(POST, USER_ID, "🔥") } returns "🔥"

        val first = async { repository.toggleReaction(POST, "❤️") }
        val second = async { repository.toggleReaction(POST, "🔥") }
        runCurrent()

        // The second tap waits until the first one finishes
        coVerify(exactly = 0) { remote.toggleReaction(POST, USER_ID, "🔥") }
        firstServerCall.complete("❤️")
        first.await()
        second.await()

        coVerify(exactly = 1) { remote.toggleReaction(POST, USER_ID, "🔥") }
        assertEquals(mapOf("🔥" to 1), postDao.rows.getValue(POST).reactions)
        assertEquals("🔥", postDao.rows.getValue(POST).myReaction)
    }

    @Test
    fun `signed out user gets an error and nothing changes`() = runTest {
        every { auth.currentUser } returns null
        postDao.rows[POST] = postEntity(POST, reactions = mapOf("❤️" to 1))

        val result = repository.toggleReaction(POST, "🔥")

        assertTrue(result is NexusResult.Error)
        assertEquals(mapOf("❤️" to 1), postDao.rows.getValue(POST).reactions)
        coVerify(exactly = 0) { remote.toggleReaction(any(), any(), any()) }
    }

    private companion object {
        const val POST = "post-1"
        const val USER_ID = "user-1"
    }
}
