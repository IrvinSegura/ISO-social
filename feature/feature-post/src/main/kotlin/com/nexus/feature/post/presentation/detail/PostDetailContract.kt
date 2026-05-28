package com.nexus.feature.post.presentation.detail

import com.nexus.feature.post.data.model.TenorResult
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail

data class PostDetailUiState(
    val post: PostDetail? = null,
    val isLoading: Boolean = true,
    val isEcoSending: Boolean = false,
    val ecoSheetOpen: Boolean = false,
    val selectedEcoType: EcoType = EcoType.TEXT,
    val gifSearchQuery: String = "",
    val gifResults: List<TenorResult> = emptyList(),
    val isSearchingGifs: Boolean = false,
    val error: String? = null,
)

sealed interface PostDetailIntent {
    data object OpenEcoSheet : PostDetailIntent
    data object CloseEcoSheet : PostDetailIntent
    data class SelectEcoType(val type: EcoType) : PostDetailIntent
    data class SendTextEco(val message: String) : PostDetailIntent
    data class SendEmojiEco(val emoji: String) : PostDetailIntent
    data class SendMediaEco(val type: EcoType, val mediaUrl: String) : PostDetailIntent
    data class SearchGifs(val query: String) : PostDetailIntent
    data object DismissError : PostDetailIntent
    data object Retry : PostDetailIntent
}

sealed interface PostDetailEvent {
    data object EcoSent : PostDetailEvent
}
