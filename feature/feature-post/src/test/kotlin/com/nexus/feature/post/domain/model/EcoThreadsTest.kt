package com.nexus.feature.post.domain.model

import com.nexus.feature.post.presentation.detail.moveReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EcoThreadsTest {

    private fun eco(id: String, parentId: String? = null, author: String = "Ana") =
        Eco(id = id, authorId = "u-$author", authorName = author, createdAt = 0L, parentId = parentId)

    @Test
    fun `replies go under their top-level eco in order`() {
        val threads = buildThreads(listOf(eco("a"), eco("b"), eco("r1", "a"), eco("r2", "a"), eco("r3", "b")))

        assertEquals(listOf("a", "b"), threads.map { it.root.id })
        assertEquals(listOf("r1", "r2"), threads[0].replies.map { it.id })
        assertEquals(listOf("r3"), threads[1].replies.map { it.id })
    }

    @Test
    fun `a reply whose root was not loaded becomes its own thread`() {
        val threads = buildThreads(listOf(eco("orphan", "gone"), eco("a")))

        assertEquals(listOf("orphan", "a"), threads.map { it.root.id })
        assertTrue(threads.all { it.replies.isEmpty() })
    }

    @Test
    fun `answering a reply stays in the same thread and names its author`() {
        assertEquals(EcoReplyTarget("a", "Luis"), eco("r1", "a", "Luis").replyTarget())
        assertEquals(EcoReplyTarget("a", "Ana"), eco("a").replyTarget())
    }

    @Test
    fun `moving a reaction updates both counts and drops empty ones`() {
        val start = mapOf("❤️" to 1, "🔥" to 2)

        assertEquals(mapOf("🔥" to 3), start.moveReaction("❤️", "🔥"))
        assertEquals(mapOf("❤️" to 2, "🔥" to 2), start.moveReaction(null, "❤️"))
        assertEquals(mapOf("🔥" to 2), start.moveReaction("❤️", null))
        assertEquals(start, start.moveReaction("🔥", "🔥"))
    }
}
