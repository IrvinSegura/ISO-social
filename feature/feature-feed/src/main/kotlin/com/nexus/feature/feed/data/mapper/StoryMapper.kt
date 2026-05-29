package com.nexus.feature.feed.data.mapper

import com.nexus.feature.feed.data.model.StoryDto
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType

fun StoryDto.toDomain(seenByCurrentUser: Boolean = false) = Story(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    type = runCatching { StoryType.valueOf(type) }.getOrDefault(StoryType.TEXT),
    mediaUrl = mediaUrl,
    text = text,
    textColor = textColor,
    backgroundGradient = runCatching { StoryGradient.valueOf(backgroundGradient) }.getOrDefault(StoryGradient.PURPLE),
    musicTitle = musicTitle,
    musicArtist = musicArtist,
    musicPreviewUrl = musicPreviewUrl,
    musicStartMs = musicStartMs,
    musicDurationMs = musicDurationMs,
    sharedPostId = sharedPostId,
    createdAt = createdAt,
    expiresAt = expiresAt,
    views = views,
    hasBeenSeen = seenByCurrentUser,
)
