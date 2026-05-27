package com.nexus.feature.post.presentation.create

import android.net.Uri

enum class PublishingStep { Idle, UploadingImage, SavingPost }

data class CreatePostUiState(
    val content: String = "",
    val contentError: String? = null,
    val selectedImageUri: Uri? = null,
    val publishingStep: PublishingStep = PublishingStep.Idle,
    val currentUserName: String = "",
    val currentUserPhotoUrl: String? = null,
    val error: String? = null,
) {
    val isPosting: Boolean get() = publishingStep != PublishingStep.Idle
}

sealed interface CreatePostIntent {
    data class ContentChanged(val value: String) : CreatePostIntent
    data class ImageSelected(val uri: Uri?) : CreatePostIntent
    data object RemoveImage : CreatePostIntent
    data object Publish : CreatePostIntent
    data object DismissError : CreatePostIntent
}

sealed interface CreatePostEvent {
    data object PostCreated : CreatePostEvent
    data object NavigateBack : CreatePostEvent
}
