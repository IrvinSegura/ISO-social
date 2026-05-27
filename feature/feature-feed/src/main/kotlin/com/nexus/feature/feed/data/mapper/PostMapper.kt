package com.nexus.feature.feed.data.mapper

import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.model.PostDto
import com.nexus.feature.feed.domain.model.Post

fun PostDto.toEntity(): PostEntity = PostEntity(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    content = content,
    circleId = circleId,
    circleName = circleName,
    ecoCount = ecoCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PostEntity.toDomain(currentUserId: String): Post = Post(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    content = content,
    circleId = circleId,
    circleName = circleName,
    ecoCount = ecoCount,
    createdAt = createdAt,
    isOwnPost = authorId == currentUserId,
)
