package com.nexus.feature.post.data.mapper

import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.PostDetail

fun EcoDto.toDomain(): Eco = Eco(
    id = id,
    authorId = authorId,
    authorName = authorName,
    message = message,
    createdAt = createdAt,
)

fun PostDto.toDetail(currentUserId: String, ecos: List<Eco>): PostDetail = PostDetail(
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
    ecos = ecos,
)
