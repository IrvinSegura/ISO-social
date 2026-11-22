package com.nexus.feature.feed.presentation

import com.nexus.core.firebase.post.ReportReason

enum class PendingPostStatus { UPLOADING, FAILED }

data class PendingPost(
    val tempId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val content: String,
    val mood: String?,
    val locationName: String?,
    val createdAt: Long,
    val status: PendingPostStatus = PendingPostStatus.UPLOADING,
)

data class FeedUiState(
    val pendingPosts: List<PendingPost> = emptyList(),
    val isEcoSending: Boolean = false,
    val ecoTargetPostId: String? = null,
    val error: String? = null,
)

sealed interface FeedIntent {
    data class OpenEcoSheet(val postId: String) : FeedIntent
    data object CloseEcoSheet : FeedIntent
    data class SendEco(val postId: String, val message: String) : FeedIntent
    data class ToggleReaction(val postId: String, val emoji: String) : FeedIntent
    data class RetryPendingPost(val tempId: String) : FeedIntent
    data class DismissPendingPost(val tempId: String) : FeedIntent
    data class NavigateToPost(val postId: String) : FeedIntent
    data object NavigateToCreatePost : FeedIntent
    data object DismissError : FeedIntent
    data class ToggleSave(val postId: String) : FeedIntent
    data class ReportPost(val postId: String, val reason: ReportReason, val details: String?) : FeedIntent

    /** The feed became visible again (back from another screen or from background). */
    data object ScreenResumed : FeedIntent

    /** Pull-to-refresh. */
    data object Refresh : FeedIntent
}

sealed interface FeedEvent {
    data class NavigateToPost(val postId: String) : FeedEvent
    data object NavigateToCreatePost : FeedEvent
    data object EcoSent : FeedEvent

    /** Short confirmation for the snackbar (saved, report sent...). */
    data class ShowMessage(val text: String) : FeedEvent

    /** Reload the paged feed from the server. */
    data object RefreshFeed : FeedEvent
}
