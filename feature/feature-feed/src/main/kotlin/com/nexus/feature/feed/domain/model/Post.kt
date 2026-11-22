package com.nexus.feature.feed.domain.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val circleId: String?,
    val circleName: String?,
    val ecoCount: Int,
    val createdAt: Long,
    val isOwnPost: Boolean,
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val mood: String? = null,
    val locationName: String? = null,
    /** A GIF or animated emoji from Giphy. */
    val gifUrl: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    val myReaction: String? = null,
)
