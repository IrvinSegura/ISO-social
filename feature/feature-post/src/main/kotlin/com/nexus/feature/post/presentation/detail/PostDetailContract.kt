package com.nexus.feature.post.presentation.detail

import com.nexus.core.firebase.post.ReportReason
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoReplyTarget
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail

data class PostDetailUiState(
    val post: PostDetail? = null,
    val isLoading: Boolean = true,
    val isEcoSending: Boolean = false,
    /** The thread and person the next eco answers; null for a new top-level eco. */
    val replyingTo: EcoReplyTarget? = null,
    // Transient message for the snackbar; it is dismissed once shown
    val error: String? = null,
    // Persistent: keeps the "Reintentar" state visible after the snackbar is dismissed
    val loadFailed: Boolean = false,
    val isSaved: Boolean = false,
    val isSavingEdit: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface PostDetailIntent {
    data class SendTextEco(val message: String) : PostDetailIntent
    data class SendEmojiEco(val emoji: String) : PostDetailIntent
    data class SendMediaEco(val type: EcoType, val mediaUrl: String) : PostDetailIntent
    /** Answer [eco]: the next eco goes into its thread and names its author. */
    data class ReplyTo(val eco: Eco) : PostDetailIntent
    data object CancelReply : PostDetailIntent
    data class ToggleEcoLike(val ecoId: String) : PostDetailIntent
    /** React to the post; the same emoji again takes the reaction back. */
    data class React(val emoji: String) : PostDetailIntent
    data object DismissError : PostDetailIntent
    data object Retry : PostDetailIntent
    data object ToggleSave : PostDetailIntent
    data class Report(val reason: ReportReason, val details: String?) : PostDetailIntent
    data class SaveEdit(val content: String, val mood: String?) : PostDetailIntent
    data object Delete : PostDetailIntent
}

sealed interface PostDetailEvent {
    /** The eco was stored; the screen clears the composer and celebrates. */
    data object EcoSent : PostDetailEvent
    data class ShowMessage(val text: String) : PostDetailEvent
    /** The edit was stored; the screen closes the editor. */
    data object EditSaved : PostDetailEvent
    /** The post no longer exists; the screen leaves. */
    data object Deleted : PostDetailEvent
}
