package com.nexus.feature.feed.data.mapper

import com.nexus.feature.feed.data.local.entity.PostEntity
import com.nexus.feature.feed.data.model.PostDto
import com.nexus.feature.feed.domain.model.Post

fun PostDto.toEntity(myReaction: String? = null): PostEntity = PostEntity(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    content = content,
    circleId = circleId,
    circleName = circleName,
    ecoCount = ecoCount,
    imageUrls = imageUrls,
    audioUrl = audioUrl,
    fileUrl = fileUrl,
    fileName = fileName,
    mood = mood,
    locationName = locationName,
    gifUrl = gifUrl,
    reactions = reactions.mapValues { (_, v) -> (v as? Number)?.toInt() ?: 0 },
    myReaction = myReaction,
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
    imageUrls = imageUrls,
    audioUrl = audioUrl,
    fileUrl = fileUrl,
    fileName = fileName,
    mood = mood,
    locationName = locationName,
    gifUrl = gifUrl,
    reactions = reactions,
    myReaction = myReaction,
)
