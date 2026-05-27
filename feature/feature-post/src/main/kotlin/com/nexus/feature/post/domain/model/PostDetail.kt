package com.nexus.feature.post.domain.model

data class PostDetail(
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
    val ecos: List<Eco>,
)
