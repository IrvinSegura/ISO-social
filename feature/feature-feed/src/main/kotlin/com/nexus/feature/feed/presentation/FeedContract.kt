package com.nexus.feature.feed.presentation

data class FeedUiState(
    val isEcoSending: Boolean = false,
    val ecoTargetPostId: String? = null,
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
