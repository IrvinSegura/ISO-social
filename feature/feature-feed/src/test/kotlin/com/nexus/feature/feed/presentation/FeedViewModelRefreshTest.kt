package com.nexus.feature.feed.presentation

import androidx.paging.PagingData
import app.cash.turbine.test
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.NewPostPayload
import com.nexus.core.firebase.post.PostActions
import com.nexus.feature.feed.domain.usecase.CacheNewPostUseCase
import com.nexus.feature.feed.domain.usecase.GetFeedUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelRefreshTest {

    private val dispatcher = StandardTestDispatcher()
    private val bus = NewPostEventBus()
    private lateinit var viewModel: FeedViewModel
    private var now = 0L
    private val cacheNewPost = mockk<CacheNewPostUseCase>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val getFeed = mockk<GetFeedUseCase>()
        every { getFeed() } returns flowOf(PagingData.empty())
        viewModel = FeedViewModel(
            getFeedUseCase = getFeed,
            sendEcoUseCase = mockk(),
            toggleReactionUseCase = mockk(),
            cacheNewPostUseCase = cacheNewPost,
            syncEcoCountUseCase = mockk(relaxed = true),
            syncReactionUseCase = mockk(relaxed = true),
            removeCachedPostUseCase = mockk(relaxed = true),
            postActions = mockk<PostActions> { every { observeSavedIds() } returns flowOf(emptySet()) },
            newPostEventBus = bus,
        )
        now = System.currentTimeMillis()
        viewModel.clock = { now }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `coming back right away does not reload the feed`() = runTest(dispatcher) {
        viewModel.events.test {
            viewModel.onIntent(FeedIntent.ScreenResumed)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `coming back after the data went stale reloads the feed once`() = runTest(dispatcher) {
        viewModel.events.test {
            now += FeedViewModel.STALE_AFTER_MS
            viewModel.onIntent(FeedIntent.ScreenResumed)
            assertEquals(FeedEvent.RefreshFeed, awaitItem())

            // The reload counts as fresh data
            viewModel.onIntent(FeedIntent.ScreenResumed)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `no reload while a post is still uploading`() = runTest(dispatcher) {
        advanceUntilIdle() // let the ViewModel subscribe to the bus before emitting
        bus.emitPending(pendingPayload())
        advanceUntilIdle()

        viewModel.events.test {
            now += FeedViewModel.STALE_AFTER_MS
            viewModel.onIntent(FeedIntent.ScreenResumed)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `pull to refresh always reloads`() = runTest(dispatcher) {
        viewModel.events.test {
            viewModel.onIntent(FeedIntent.Refresh)
            assertEquals(FeedEvent.RefreshFeed, awaitItem())
        }
    }

    @Test
    fun `a post published in a circle never shows in the public feed`() = runTest(dispatcher) {
        advanceUntilIdle()
        bus.emitPending(pendingPayload(tempId = "circle-1", circleId = "c1"))
        bus.emitPending(pendingPayload(tempId = "public-1"))
        advanceUntilIdle()
        assertEquals(listOf("public-1"), viewModel.uiState.value.pendingPosts.map { it.tempId })

        bus.emitConfirmed("circle-1", "real-circle-post")
        advanceUntilIdle()
        coVerify(exactly = 0) { cacheNewPost("real-circle-post") }
    }

    private fun pendingPayload(tempId: String = "temp-1", circleId: String? = null) = NewPostPayload(
        circleId = circleId,
        tempId = tempId,
        authorId = "author",
        authorName = "Author",
        authorPhotoUrl = null,
        content = "hello",
        mood = null,
        locationName = null,
        createdAt = 1L,
        isPending = true,
    )
}
