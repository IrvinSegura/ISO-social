package com.nexus.feature.feed.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// The index serves the feed query (ORDER BY createdAt DESC, id DESC); see FeedMigrations
@Entity(tableName = "posts", indices = [Index(value = ["createdAt", "id"])])
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val circleId: String?,
    val circleName: String?,
    val ecoCount: Int,
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val mood: String? = null,
    val locationName: String? = null,
    val gifUrl: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    // Emoji the current user reacted with, so it survives refreshes and restarts
    val myReaction: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
