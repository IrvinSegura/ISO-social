package com.nexus.feature.circles.presentation.detail

import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.CirclePost

enum class CircleTab { POSTS, MEMBERS }

/** A post published in this circle that the server has not confirmed yet. */
data class PendingCirclePost(
    val tempId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val createdAt: Long,
    val failed: Boolean = false,
)

data class CircleDetailState(
    val circle: CircleSummary? = null,
    val isLoading: Boolean = true,
    val currentUid: String? = null,
    val selectedTab: CircleTab = CircleTab.POSTS,
    val posts: List<CirclePost> = emptyList(),
    val pending: List<PendingCirclePost> = emptyList(),
    val postsLoading: Boolean = true,
    val postsError: String? = null,
    val members: List<CircleMember> = emptyList(),
    val membersLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** A leave, delete, kick or code change is being saved. */
    val isBusy: Boolean = false,
) {
    val isOwner: Boolean get() = circle != null && circle.ownerId == currentUid
}

sealed interface CircleDetailIntent {
    data class SelectTab(val tab: CircleTab) : CircleDetailIntent
    data object Refresh : CircleDetailIntent
    data object RetryPosts : CircleDetailIntent
    data object Leave : CircleDetailIntent
    data object Delete : CircleDetailIntent
    data class RemoveMember(val uid: String) : CircleDetailIntent
    data object RegenerateCode : CircleDetailIntent
    data class RetryPending(val tempId: String) : CircleDetailIntent
    data class DiscardPending(val tempId: String) : CircleDetailIntent
}

sealed interface CircleDetailEvent {
    data class ShowMessage(val text: String) : CircleDetailEvent
    /** The user is no longer in the circle (left, removed or it was deleted): leave the screen. */
    data class Closed(val message: String) : CircleDetailEvent
}
