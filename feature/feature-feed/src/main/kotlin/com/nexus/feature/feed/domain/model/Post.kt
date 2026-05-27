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
)
