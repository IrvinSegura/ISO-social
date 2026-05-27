package com.nexus.feature.feed.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val circleId: String?,
    val circleName: String?,
    val ecoCount: Int,
    val imageUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
