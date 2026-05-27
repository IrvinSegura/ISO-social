package com.nexus.feature.post.presentation.create

data class CreatePostUiState(
    val content: String = "",
    val contentError: String? = null,
    val isPosting: Boolean = false,
    val error: String? = null,
)

sealed interface CreatePostIntent {
    data class ContentChanged(val value: String) : CreatePostIntent
    data object Publish : CreatePostIntent
    data object DismissError : CreatePostIntent
}

sealed interface CreatePostEvent {
    data object PostCreated : CreatePostEvent
    data object NavigateBack : CreatePostEvent
}
