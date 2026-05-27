package com.nexus.feature.post.domain.model

data class Eco(
    val id: String,
    val authorId: String,
    val authorName: String,
    val message: String,
    val createdAt: Long,
)
