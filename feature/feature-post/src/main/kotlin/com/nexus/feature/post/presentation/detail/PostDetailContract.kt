package com.nexus.feature.post.presentation.detail

import com.nexus.feature.post.domain.model.PostDetail

data class PostDetailUiState(
    val post: PostDetail? = null,
    val isLoading: Boolean = true,
    val isEcoSending: Boolean = false,
    val ecoSheetOpen: Boolean = false,
    val error: String? = null,
)

sealed interface PostDetailIntent {
    data object OpenEcoSheet : PostDetailIntent
    data object CloseEcoSheet : PostDetailIntent
    data class SendEco(val message: String) : PostDetailIntent
    data object DismissError : PostDetailIntent
    data object Retry : PostDetailIntent
}

sealed interface PostDetailEvent {
    data object EcoSent : PostDetailEvent
}
