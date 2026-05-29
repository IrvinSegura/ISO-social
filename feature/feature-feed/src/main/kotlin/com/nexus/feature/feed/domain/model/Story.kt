package com.nexus.feature.feed.domain.model

data class Story(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val type: StoryType,
    val mediaUrl: String?,
    val text: String?,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundGradient: StoryGradient = StoryGradient.PURPLE,
    val musicTitle: String?,
    val musicArtist: String?,
    val musicPreviewUrl: String? = null,
    val musicStartMs: Long = 0L,
    val musicDurationMs: Long = 30_000L,
    val sharedPostId: String?,
    val createdAt: Long,
    val expiresAt: Long,
    val views: Int = 0,
    val hasBeenSeen: Boolean = false,
)

enum class StoryType { IMAGE, VIDEO, TEXT, REEL, SHARED_POST }

enum class StoryGradient(val colors: List<Long>) {
    PURPLE(listOf(0xFF6C63FF, 0xFF4A43CC)),
    TEAL(listOf(0xFF00D4AA, 0xFF00A884)),
    SUNSET(listOf(0xFFFF6B6B, 0xFFFFD93D)),
    OCEAN(listOf(0xFF4776E6, 0xFF8E54E9)),
    DARK(listOf(0xFF1A1A2E, 0xFF252540)),
}
