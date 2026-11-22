package com.nexus.feature.feed.data.local.entity

/** Partial row of [PostEntity]: the emoji the current user reacted with on a post. */
data class PostReactionRow(
    val id: String,
    val myReaction: String?,
)
