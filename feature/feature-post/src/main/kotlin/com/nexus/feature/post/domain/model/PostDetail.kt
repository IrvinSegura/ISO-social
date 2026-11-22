package com.nexus.feature.post.domain.model

data class PostDetail(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val circleId: String?,
    val circleName: String?,
    val ecoCount: Int,
    val imageUrls: List<String>,
    val audioUrl: String?,
    val fileUrl: String?,
    val fileName: String?,
    val mood: String?,
    val locationName: String?,
    /** A GIF or animated emoji from Giphy. */
    val gifUrl: String? = null,
    val reactions: Map<String, Int>,
    val createdAt: Long,
    val isOwnPost: Boolean,
    val ecos: List<Eco>,
    /** The current user's reaction, or null when they have not reacted. */
    val myReaction: String? = null,
) {
    val threads: List<EcoThread> get() = buildThreads(ecos)
}
