package com.nexus.feature.post.domain.model

enum class EcoType { TEXT, EMOJI, GIF, IMAGE, AUDIO }

data class Eco(
    val id: String,
    val authorId: String,
    val authorName: String,
    val type: EcoType = EcoType.TEXT,
    val message: String = "",
    val emoji: String? = null,
    val mediaUrl: String? = null,
    val createdAt: Long,
)
