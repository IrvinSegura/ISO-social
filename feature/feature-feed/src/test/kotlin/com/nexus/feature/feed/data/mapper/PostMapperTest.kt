package com.nexus.feature.feed.data.mapper

import com.nexus.feature.feed.data.postDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostMapperTest {

    @Test
    fun `Firestore numbers of any type become Int counters`() {
        // Firestore returns Long for integers and may return Double after some writes
        val dto = postDto("p1", createdAt = 10L, reactions = mapOf("❤️" to 3L, "🔥" to 2.0, "😂" to "bad"))

        val entity = dto.toEntity()

        assertEquals(mapOf("❤️" to 3, "🔥" to 2, "😂" to 0), entity.reactions)
    }

    @Test
    fun `the user's reaction is stored only when given`() {
        val dto = postDto("p1", createdAt = 10L)

        assertNull(dto.toEntity().myReaction)
        assertEquals("🔥", dto.toEntity(myReaction = "🔥").myReaction)
    }

    @Test
    fun `entity to domain keeps the fields and marks own posts`() {
        val entity = postDto("p1", createdAt = 10L).toEntity(myReaction = "❤️")

        val own = entity.toDomain(currentUserId = "author")
        val other = entity.toDomain(currentUserId = "someone-else")

        assertTrue(own.isOwnPost)
        assertFalse(other.isOwnPost)
        assertEquals("p1", own.id)
        assertEquals(10L, own.createdAt)
        assertEquals("❤️", own.myReaction)
    }
}
