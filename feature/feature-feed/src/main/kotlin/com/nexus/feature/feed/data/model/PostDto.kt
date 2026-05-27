package com.nexus.feature.feed.data.model

data class PostDto(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String? = null,
    val content: String = "",
    val circleId: String? = null,
    val circleName: String? = null,
    val ecoCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
