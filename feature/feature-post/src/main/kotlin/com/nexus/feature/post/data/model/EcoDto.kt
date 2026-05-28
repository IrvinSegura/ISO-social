package com.nexus.feature.post.data.model

data class EcoDto(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val type: String = "TEXT",
    val message: String = "",
    val emoji: String? = null,
    val mediaUrl: String? = null,
    val createdAt: Long = 0L,
)
