package com.nexus.feature.feed.presentation.ui.story

import com.nexus.feature.feed.domain.model.StoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoryTypeForTest {

    @Test
    fun `a video picked in story mode is saved as VIDEO, not IMAGE`() {
        assertEquals(StoryType.VIDEO, storyTypeFor(CreatorMode.HISTORY, hasMedia = true, isVideo = true))
    }

    @Test
    fun `an image picked in story mode is saved as IMAGE`() {
        assertEquals(StoryType.IMAGE, storyTypeFor(CreatorMode.HISTORY, hasMedia = true, isVideo = false))
    }

    @Test
    fun `story mode without media is a TEXT story`() {
        assertEquals(StoryType.TEXT, storyTypeFor(CreatorMode.HISTORY, hasMedia = false, isVideo = false))
    }

    @Test
    fun `a video in reel mode is a REEL`() {
        assertEquals(StoryType.REEL, storyTypeFor(CreatorMode.REEL, hasMedia = true, isVideo = true))
    }

    @Test
    fun `reel mode with an image or no media cannot be published`() {
        assertNull(storyTypeFor(CreatorMode.REEL, hasMedia = true, isVideo = false))
        assertNull(storyTypeFor(CreatorMode.REEL, hasMedia = false, isVideo = false))
    }
}
