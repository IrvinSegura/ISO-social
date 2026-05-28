package com.nexus.feature.post.data.mapper

import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail

fun EcoDto.toDomain(): Eco = Eco(
    id = id,
    authorId = authorId,
    authorName = authorName,
    type = try { EcoType.valueOf(type) } catch (_: Exception) { EcoType.TEXT },
    message = message,
    emoji = emoji,
    mediaUrl = mediaUrl,
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
    imageUrls = imageUrls,
    audioUrl = audioUrl,
    fileUrl = fileUrl,
    fileName = fileName,
    mood = mood,
    locationName = locationName,
    reactions = reactions.mapValues { (_, v) -> (v as? Number)?.toInt() ?: 0 },
    createdAt = createdAt,
    isOwnPost = authorId == currentUserId,
    ecos = ecos,
)
