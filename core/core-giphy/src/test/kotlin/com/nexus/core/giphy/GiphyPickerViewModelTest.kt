package com.nexus.core.giphy

import com.nexus.core.giphy.ui.GiphyPickerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GiphyPickerViewModelTest {

    /** Records every call; each result is one GIF whose id names the call. */
    private class FakeApi : GiphyApi {
        val calls = mutableListOf<String>()
        var fail = false

        private fun respond(call: String): GiphyResponse {
            calls += call
            if (fail) error("offline")
            return GiphyResponse(listOf(gif(call)))
        }

        override suspend fun trendingGifs(apiKey: String, limit: Int, rating: String) = respond("trending")
        override suspend fun searchGifs(query: String, apiKey: String, limit: Int, rating: String, lang: String) =
            respond("gifs:$query")
        override suspend fun emoji(apiKey: String, limit: Int) = respond("emoji")
        override suspend fun searchStickers(query: String, apiKey: String, limit: Int, rating: String, lang: String) =
            respond("stickers:$query")
    }

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(key: String = "key"): GiphyPickerViewModel =
        GiphyPickerViewModel(GiphyRepository(api, key)).also { advanceUntilIdle() }

    @Test
    fun `opens with trending GIFs and switches to Giphy emoji`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(listOf("trending"), vm.state.value.items.map { it.id })

        vm.onKindChanged(GiphyKind.EMOJI)
        advanceUntilIdle()

        assertEquals(listOf("trending", "emoji"), api.calls)
        assertEquals(listOf("emoji"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `typing searches once, after the pause, with the last text`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKindChanged(GiphyKind.EMOJI)
        advanceUntilIdle()

        vm.onQueryChanged("ga")
        advanceTimeBy(100)
        vm.onQueryChanged("gato")
        advanceUntilIdle()

        assertEquals(listOf("trending", "emoji", "stickers:gato"), api.calls)
        assertEquals("stickers:gato", vm.state.value.items.single().id)
    }

    @Test
    fun `a failed search shows the error and retry loads again`() = runTest(dispatcher) {
        api.fail = true
        val vm = viewModel()
        assertTrue(vm.state.value.hasError)

        api.fail = false
        vm.retry()
        advanceUntilIdle()

        assertFalse(vm.state.value.hasError)
        assertEquals(listOf("trending"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `without an API key nothing is requested`() = runTest(dispatcher) {
        val vm = viewModel(key = "")

        assertFalse(vm.state.value.isAvailable)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun `only Giphy URLs are kept`() {
        assertTrue(GiphyRepository.isGiphyUrl("https://media2.giphy.com/media/abc/giphy.gif?cid=1"))
        assertFalse(GiphyRepository.isGiphyUrl("https://evil.com/giphy.com/x.gif"))
        assertFalse(GiphyRepository.isGiphyUrl("http://media.giphy.com/media/abc/giphy.gif"))
        assertEquals(null, GiphyGifDto(id = "x", images = GiphyImages(original = GiphyRendition("https://evil.com/a.gif"))).toItem())
    }

    private companion object {
        fun gif(id: String) = GiphyGifDto(
            id = id,
            images = GiphyImages(
                fixedWidthSmall = GiphyRendition("https://media.giphy.com/media/$id/100w.gif"),
                downsized = GiphyRendition("https://media.giphy.com/media/$id/giphy.gif"),
            ),
        )
    }
}
