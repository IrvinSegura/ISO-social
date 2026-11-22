package com.nexus.feature.profile.domain.model

/** The signed-in user as the profile shows them. */
data class ProfileUser(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    val email: String?,
    /** When the account was created, if Firebase knows it. */
    val memberSince: Long?,
)

/** A compact view of a post: enough for a list row, not the whole detail. */
data class ProfilePost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val imageUrl: String?,
    val imageCount: Int,
    val hasAudio: Boolean,
    val fileName: String?,
    val mood: String?,
    val ecoCount: Int,
    val reactionCount: Int,
    val createdAt: Long,
)

/** Totals over the user's own posts. */
data class ProfileStats(
    val posts: Int = 0,
    val ecosReceived: Int = 0,
    val reactionsReceived: Int = 0,
    /** True when only the newest posts were loaded, so the totals are a lower bound. */
    val isPartial: Boolean = false,
) {
    companion object {
        fun from(posts: List<ProfilePost>, isPartial: Boolean) = ProfileStats(
            posts = posts.size,
            ecosReceived = posts.sumOf { it.ecoCount },
            reactionsReceived = posts.sumOf { it.reactionCount },
            isPartial = isPartial,
        )
    }
}

/** The user's posts, newest first; [hasMore] when the list was cut at the page size. */
data class MyPosts(val posts: List<ProfilePost>, val hasMore: Boolean)
