package com.nexus.feature.feed.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class StoryDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val text: String? = null,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundGradient: String = "PURPLE",
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicPreviewUrl: String? = null,
    val musicStartMs: Long = 0L,
    val musicDurationMs: Long = 30_000L,
    val sharedPostId: String? = null,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    // Same instant as expiresAt, as a Timestamp: the Firestore TTL policy deletes by it
    val expireAt: Timestamp? = null,
    val views: Int = 0,
)
