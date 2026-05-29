package com.nexus.feature.feed.presentation

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
    val ecoCountDeltas: Map<String, Int> = emptyMap(), // postId → delta local
    val isEcoSending: Boolean = false,
    val ecoTargetPostId: String? = null,
    val userReactions: Map<String, String> = emptyMap(), // postId → emoji seleccionado
    val error: String? = null,
)

sealed interface FeedIntent {
    data class OpenEcoSheet(val postId: String) : FeedIntent
    data object CloseEcoSheet : FeedIntent
    data class SendEco(val postId: String, val message: String) : FeedIntent
    data class ToggleReaction(val postId: String, val emoji: String) : FeedIntent
    data class NavigateToPost(val postId: String) : FeedIntent
    data object NavigateToCreatePost : FeedIntent
    data object DismissError : FeedIntent
}

sealed interface FeedEvent {
    data class NavigateToPost(val postId: String) : FeedEvent
    data object NavigateToCreatePost : FeedEvent
    data object EcoSent : FeedEvent
}
