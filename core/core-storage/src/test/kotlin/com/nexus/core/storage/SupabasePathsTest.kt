package com.nexus.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabasePathsTest {

    private val paths = SupabasePaths("https://abc.supabase.co/", "media")

    @Test
    fun `public url keeps folders and drops the trailing slash of the project url`() {
        assertEquals(
            "https://abc.supabase.co/storage/v1/object/public/media/stories/uid1/a.jpg",
            paths.publicUrl("stories/uid1/a.jpg"),
        )
    }

    @Test
    fun `object url is the authenticated endpoint`() {
        assertEquals(
            "https://abc.supabase.co/storage/v1/object/media/posts/uid1/p1/audio.m4a",
            paths.objectUrl("posts/uid1/p1/audio.m4a"),
        )
    }

    @Test
    fun `spaces in a file name become percent-encoded, not plus signs`() {
        assertEquals(
            "https://abc.supabase.co/storage/v1/object/public/media/posts/u/p/files/mi%20cv.pdf",
            paths.publicUrl("posts/u/p/files/mi cv.pdf"),
        )
    }

    @Test
    fun `a blank or non https project url is not configured`() {
        assertFalse(SupabasePaths("", "media").isConfigured)
        assertFalse(SupabasePaths("http://abc.supabase.co", "media").isConfigured)
        assertTrue(paths.isConfigured)
    }

    @Test
    fun `a public url maps back to its path, other urls do not`() {
        val path = "posts/u/p/files/mi cv+1.pdf"
        assertEquals(path, paths.pathOf(paths.publicUrl(path)))
        assertEquals(null, paths.pathOf("https://evil.example.com/storage/v1/object/public/media/posts/u/p/a.jpg"))
        assertEquals(null, SupabasePaths("", "media").pathOf("posts/u/p/a.jpg"))
    }
}
