package com.nexus.feature.circles

import androidx.lifecycle.SavedStateHandle
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.NewPostPayload
import com.nexus.feature.circles.FakeCirclesRepository.Companion.circle
import com.nexus.feature.circles.FakeCirclesRepository.Companion.post
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.domain.model.InvitePreview
import com.nexus.feature.circles.presentation.detail.CircleDetailEvent
import com.nexus.feature.circles.presentation.detail.CircleDetailIntent
import com.nexus.feature.circles.presentation.detail.CircleDetailViewModel
import com.nexus.feature.circles.presentation.list.CirclesEvent
import com.nexus.feature.circles.presentation.list.CirclesIntent
import com.nexus.feature.circles.presentation.list.CirclesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CirclesViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collect(flow: kotlinx.coroutines.flow.Flow<T>): List<T> {
        val events = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(events) }
        return events
    }

    // ── Invite codes ────────────────────────────────────────────────────

    @Test
    fun `invite codes are normalized and validated`() {
        assertEquals("ABCD2345", InviteCode.normalize(" abcd-2345 "))
        assertTrue(InviteCode.isValid("ABCD2345"))
        assertFalse("no O or 0", InviteCode.isValid("ABCD2340"))
        assertFalse("too short", InviteCode.isValid("ABCD234"))
        assertEquals("ABCD-2345", InviteCode.format("ABCD2345"))
    }

    // ── List ────────────────────────────────────────────────────────────

    @Test
    fun `lists only the circles the user belongs to`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf(
                "a" to circle("a", "Familia", ownerId = "me", members = listOf("me")),
                "b" to circle("b", "Ajeno", ownerId = "bob", members = listOf("bob")),
            )
        }
        val vm = CirclesViewModel(repo)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("a"), vm.state.value.circles.map { it.id })
    }

    @Test
    fun `creating a circle opens it`() = runTest {
        val repo = FakeCirclesRepository()
        val vm = CirclesViewModel(repo)
        val events = collect(vm.events)

        vm.onIntent(CirclesIntent.Create("  Equipo  ", "💼", 2))
        advanceUntilIdle()

        assertEquals(listOf("create:Equipo"), repo.calls)
        assertEquals(CirclesEvent.OpenCircle("c1"), events.single())
        assertFalse(vm.state.value.isCreating)
    }

    @Test
    fun `a blank name creates nothing`() = runTest {
        val repo = FakeCirclesRepository()
        val vm = CirclesViewModel(repo)

        vm.onIntent(CirclesIntent.Create("   ", "💼", 2))
        advanceUntilIdle()

        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `a malformed code is rejected without a lookup`() = runTest {
        val repo = FakeCirclesRepository().apply { failWith = IllegalStateException("must not be called") }
        val vm = CirclesViewModel(repo)

        vm.onIntent(CirclesIntent.LookUpCode("abc"))

        assertNotNull(vm.state.value.join.error)
        assertNull(vm.state.value.join.preview)
    }

    @Test
    fun `an unknown code says so`() = runTest {
        val vm = CirclesViewModel(FakeCirclesRepository())

        vm.onIntent(CirclesIntent.LookUpCode("ABCD-2345"))
        advanceUntilIdle()

        assertNotNull(vm.state.value.join.error)
        assertFalse(vm.state.value.join.isSearching)
    }

    @Test
    fun `joining with a code adds the user and opens the circle`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("b" to circle("b", "Club", ownerId = "bob", members = listOf("bob")))
            invites["ABCD2345"] = InvitePreview("ABCD2345", "b", "Club", "📚", 1)
        }
        val vm = CirclesViewModel(repo)
        val events = collect(vm.events)

        vm.onIntent(CirclesIntent.LookUpCode("abcd 2345"))
        advanceUntilIdle()
        assertEquals("Club", vm.state.value.join.preview?.name)

        vm.onIntent(CirclesIntent.ConfirmJoin)
        advanceUntilIdle()

        assertEquals(listOf("join:b"), repo.calls)
        assertEquals(CirclesEvent.OpenCircle("b"), events.single())
        assertNull(vm.state.value.join.preview)
        assertEquals(listOf("b"), vm.state.value.circles.map { it.id })
    }

    @Test
    fun `joining a circle the user is already in only opens it`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Familia", ownerId = "me", members = listOf("me")))
            invites["ABCD2345"] = InvitePreview("ABCD2345", "a", "Familia", "🏠", 0)
        }
        val vm = CirclesViewModel(repo)
        val events = collect(vm.events)

        vm.onIntent(CirclesIntent.LookUpCode("ABCD2345"))
        advanceUntilIdle()
        vm.onIntent(CirclesIntent.ConfirmJoin)
        advanceUntilIdle()

        assertTrue(repo.calls.isEmpty())
        assertEquals(CirclesEvent.OpenCircle("a"), events.single())
    }

    @Test
    fun `a failed join keeps the preview and explains why`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("b" to circle("b", "Club", ownerId = "bob", members = listOf("bob")))
            invites["ABCD2345"] = InvitePreview("ABCD2345", "b", "Club", "📚", 1)
        }
        val vm = CirclesViewModel(repo)
        vm.onIntent(CirclesIntent.LookUpCode("ABCD2345"))
        advanceUntilIdle()

        repo.failWith = IllegalStateException("PERMISSION_DENIED")
        vm.onIntent(CirclesIntent.ConfirmJoin)
        advanceUntilIdle()

        val join = vm.state.value.join
        assertNotNull(join.preview)
        assertNotNull(join.error)
        assertFalse(join.isJoining)
    }

    // ── Detail ──────────────────────────────────────────────────────────

    private fun detail(repo: FakeCirclesRepository, bus: NewPostEventBus = NewPostEventBus(), id: String = "a") =
        CircleDetailViewModel(SavedStateHandle(mapOf("circleId" to id)), repo, bus)

    @Test
    fun `detail loads the circle, its posts and members`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Familia", ownerId = "me", members = listOf("me", "bob")))
            posts["a"] = listOf(post("p1"), post("p2"))
        }
        val vm = detail(repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Familia", state.circle?.name)
        assertTrue(state.isOwner)
        assertEquals(listOf("p1", "p2"), state.posts.map { it.id })
        assertEquals(listOf("me", "bob"), state.members.map { it.uid })
    }

    @Test
    fun `leaving closes the screen with its own message`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Club", ownerId = "bob", members = listOf("bob", "me")))
        }
        val vm = detail(repo)
        val events = collect(vm.events)
        advanceUntilIdle()

        vm.onIntent(CircleDetailIntent.Leave)
        advanceUntilIdle()

        assertEquals(listOf("leave:a"), repo.calls.filter { it.startsWith("leave") })
        assertEquals(listOf(CircleDetailEvent.Closed("Saliste de Club")), events)
    }

    @Test
    fun `the owner cannot leave, only delete`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Familia", ownerId = "me", members = listOf("me")))
        }
        val vm = detail(repo)
        val events = collect(vm.events)
        advanceUntilIdle()

        vm.onIntent(CircleDetailIntent.Leave)
        advanceUntilIdle()
        assertTrue(repo.calls.none { it.startsWith("leave") })

        vm.onIntent(CircleDetailIntent.Delete)
        advanceUntilIdle()
        assertTrue("delete:a" in repo.calls)
        assertEquals(listOf(CircleDetailEvent.Closed("Eliminaste Familia")), events)
    }

    @Test
    fun `being removed by the owner closes the screen`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Club", ownerId = "bob", members = listOf("bob", "me")))
        }
        val vm = detail(repo)
        val events = collect(vm.events)
        advanceUntilIdle()

        repo.circles.value += "a" to circle("a", "Club", ownerId = "bob", members = listOf("bob"))
        advanceUntilIdle()

        assertEquals(listOf(CircleDetailEvent.Closed("Ya no formas parte de este círculo")), events)
    }

    @Test
    fun `the owner removes a member and the list follows`() = runTest {
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Familia", ownerId = "me", members = listOf("me", "bob")))
        }
        val vm = detail(repo)
        val events = collect(vm.events)
        advanceUntilIdle()

        vm.onIntent(CircleDetailIntent.RemoveMember("bob"))
        advanceUntilIdle()

        assertTrue("remove:bob" in repo.calls)
        assertEquals(listOf("me"), vm.state.value.members.map { it.uid })
        assertTrue(events.single() is CircleDetailEvent.ShowMessage)
    }

    @Test
    fun `pending posts of this circle show until the server confirms them`() = runTest {
        val bus = NewPostEventBus()
        val repo = FakeCirclesRepository().apply {
            circles.value = mapOf("a" to circle("a", "Familia", ownerId = "me", members = listOf("me")))
        }
        val vm = detail(repo, bus)
        advanceUntilIdle()

        bus.emitPending(payload("t1", circleId = "a"))
        bus.emitPending(payload("t2", circleId = null))
        bus.emitPending(payload("t3", circleId = "other"))
        advanceUntilIdle()
        assertEquals(listOf("t1"), vm.state.value.pending.map { it.tempId })

        bus.emitFailed("t1")
        advanceUntilIdle()
        assertTrue(vm.state.value.pending.single().failed)

        repo.posts["a"] = listOf(post("real"))
        bus.emitConfirmed("t1", "real")
        advanceUntilIdle()
        assertTrue(vm.state.value.pending.isEmpty())
        assertEquals(listOf("real"), vm.state.value.posts.map { it.id })
    }

    private fun payload(tempId: String, circleId: String?) = NewPostPayload(
        tempId = tempId, authorId = "me", authorName = "Yo", authorPhotoUrl = null, content = "Hola",
        mood = null, locationName = null, createdAt = 0, isPending = true, circleId = circleId,
    )
}
