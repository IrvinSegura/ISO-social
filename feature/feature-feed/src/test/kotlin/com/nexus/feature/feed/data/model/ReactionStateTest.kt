package com.nexus.feature.feed.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReactionStateTest {

    @Test
    fun `tapping a new emoji adds it and counts it`() {
        val state = ReactionState(mapOf("🔥" to 2), myReaction = null)

        val result = state.toggle("❤️")

        assertEquals(ReactionState(mapOf("🔥" to 2, "❤️" to 1), "❤️"), result)
    }

    @Test
    fun `tapping the current emoji removes it and drops a zero counter`() {
        val state = ReactionState(mapOf("❤️" to 1), myReaction = "❤️")

        val result = state.toggle("❤️")

        assertEquals(ReactionState(emptyMap(), null), result)
    }

    @Test
    fun `tapping another emoji moves the reaction between counters`() {
        val state = ReactionState(mapOf("❤️" to 3, "🔥" to 1), myReaction = "❤️")

        val result = state.toggle("🔥")

        assertEquals(ReactionState(mapOf("❤️" to 2, "🔥" to 2), "🔥"), result)
    }

    @Test
    fun `switching to the current reaction changes nothing`() {
        val state = ReactionState(mapOf("❤️" to 1), myReaction = "❤️")

        assertSame(state, state.switchTo("❤️"))
    }

    @Test
    fun `switching back undoes a toggle`() {
        val before = ReactionState(mapOf("❤️" to 3, "🔥" to 1), myReaction = "❤️")

        val undone = before.toggle("🔥").switchTo(before.myReaction)

        assertEquals(before, undone)
    }
}
