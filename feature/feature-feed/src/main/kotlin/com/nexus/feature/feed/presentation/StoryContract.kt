package com.nexus.feature.feed.presentation

import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType

data class StoryUiState(
    val stories: List<Story> = emptyList(),
    val isCreating: Boolean = false,
    val error: String? = null,
)

sealed interface StoryIntent {
    data class CreateStory(
        val type: StoryType,
        val mediaLocalUri: String?,
        val text: String?,
        val gradient: StoryGradient,
        val musicTitle: String?,
        val musicArtist: String?,
        val musicPreviewUrl: String? = null,
        val musicStartMs: Long = 0L,
        val musicDurationMs: Long = 30_000L,
    ) : StoryIntent
    data class MarkSeen(val storyId: String) : StoryIntent
    data object DismissError : StoryIntent
}

sealed interface StoryEvent {
    data object StoryCreated : StoryEvent
    data class ShowError(val message: String) : StoryEvent
}
