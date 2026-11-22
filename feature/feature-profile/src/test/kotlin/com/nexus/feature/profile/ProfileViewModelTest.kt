package com.nexus.feature.profile

import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.PostChange
import com.nexus.core.datastore.AppPreferencesDataStore
import com.nexus.core.datastore.model.ThemeMode
import com.nexus.feature.profile.domain.model.MyPosts
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.domain.model.ProfileUser
import com.nexus.feature.profile.domain.repository.ProfileRepository
import com.nexus.feature.profile.presentation.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ProfileViewModelTest {

    private class FakeRepository : ProfileRepository {
        val posts = mutableMapOf<String, ProfilePost>()
        val savedIds = MutableStateFlow<List<String>>(emptyList())
        var mine: List<ProfilePost> = emptyList()
        val fetchedIds = mutableListOf<List<String>>()

        override suspend fun currentUser() = ProfileUser("me", "Yo", null, "yo@example.com", null)
        override suspend fun myPosts() = MyPosts(mine, hasMore = false)
        override suspend fun postsByIds(ids: List<String>): List<ProfilePost> {
            fetchedIds += ids
            return ids.mapNotNull { posts[it] }
        }
        override fun observeSavedIds(): Flow<List<String>> = savedIds
        override suspend fun setSaved(postId: String, saved: Boolean) {
            savedIds.value = if (saved) listOf(postId) + savedIds.value - postId else savedIds.value - postId
        }
    }

    private fun post(id: String, ecos: Int = 0, reactions: Int = 0) = ProfilePost(
        id = id, authorId = "me", authorName = "Yo", authorPhotoUrl = null, content = "Hola $id",
        imageUrl = null, imageCount = 0, hasAudio = false, fileName = null, mood = null,
        ecoCount = ecos, reactionCount = reactions, createdAt = 0L,
    )

    private val preferences = mockk<AppPreferencesDataStore> {
        every { themeMode } returns flowOf(ThemeMode.DARK)
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `stats add up ecos and reactions of own posts`() = runTest {
        val repo = FakeRepository().apply { mine = listOf(post("a", ecos = 2, reactions = 5), post("b", ecos = 1)) }
        val vm = ProfileViewModel(repo, preferences, NewPostEventBus())
        advanceUntilIdle()

        val stats = vm.state.value.stats
        assertEquals(2, stats.posts)
        assertEquals(3, stats.ecosReceived)
        assertEquals(5, stats.reactionsReceived)
        assertEquals(ThemeMode.DARK, vm.state.value.themeMode)
    }

    @Test
    fun `saved list keeps saved order and only fetches new ids`() = runTest {
        val repo = FakeRepository().apply {
            posts += listOf("x", "y", "z").associateWith { post(it) }
            savedIds.value = listOf("y", "x")
        }
        val vm = ProfileViewModel(repo, preferences, NewPostEventBus())
        advanceUntilIdle()
        assertEquals(listOf("y", "x"), vm.state.value.saved.items.map { it.id })

        repo.savedIds.value = listOf("z", "y", "x")
        advanceUntilIdle()
        assertEquals(listOf("z", "y", "x"), vm.state.value.saved.items.map { it.id })
        assertEquals(listOf(listOf("y", "x"), listOf("z")), repo.fetchedIds)
    }

    @Test
    fun `saved ids of deleted posts are dropped`() = runTest {
        val repo = FakeRepository().apply {
            posts["x"] = post("x")
            savedIds.value = listOf("gone", "x")
        }
        val vm = ProfileViewModel(repo, preferences, NewPostEventBus())
        advanceUntilIdle()

        assertEquals(listOf("x"), vm.state.value.saved.items.map { it.id })
        assertEquals(listOf("x"), repo.savedIds.value)
        assertFalse(vm.state.value.saved.isLoading)
    }

    @Test
    fun `a deleted post leaves both lists and the stats`() = runTest {
        val bus = NewPostEventBus()
        val repo = FakeRepository().apply {
            mine = listOf(post("a", ecos = 4), post("b"))
            posts["a"] = mine[0]
            savedIds.value = listOf("a")
        }
        val vm = ProfileViewModel(repo, preferences, bus)
        advanceUntilIdle()

        bus.emitPostChange(PostChange.Deleted("a"))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(listOf("b"), state.myPosts.items.map { it.id })
        assertTrue(state.saved.items.isEmpty())
        assertEquals(0, state.stats.ecosReceived)
    }
}
