package com.nexus.feature.post.presentation.create

import android.net.Uri
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.giphy.GiphyKind

private const val MAX_IMAGES = 4

enum class PublishingStep {
    Idle, UploadingImages, UploadingAudio, UploadingFile, SavingPost;

    val label: String get() = when (this) {
        Idle -> ""
        UploadingImages -> "Subiendo imágenes..."
        UploadingAudio -> "Subiendo audio..."
        UploadingFile -> "Subiendo archivo..."
        SavingPost -> "Publicando..."
    }
}

enum class RecordingState { Idle, Recording, Recorded }

data class CreatePostUiState(
    val content: String = "",
    val contentError: String? = null,
    // Images
    val selectedImageUris: List<Uri> = emptyList(),
    // Audio
    val recordingState: RecordingState = RecordingState.Idle,
    val recordingDurationMs: Long = 0L,
    val recordedAudioPath: String? = null,
    // File
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    // GIF or animated emoji from Giphy; the picker is open while giphyPicker is not null
    val gifUrl: String? = null,
    val giphyPicker: GiphyKind? = null,
    // Mood
    val selectedMood: String? = null,
    val showMoodPicker: Boolean = false,
    // Location
    val locationName: String? = null,
    val isLoadingLocation: Boolean = false,
    // Audience: null is public; otherwise the id of one of the user's circles
    val circles: List<CircleSummary> = emptyList(),
    val selectedCircleId: String? = null,
    val showAudiencePicker: Boolean = false,
    // Publishing
    val publishingStep: PublishingStep = PublishingStep.Idle,
    // User info
    val currentUserName: String = "",
    val currentUserPhotoUrl: String? = null,
    val error: String? = null,
) {
    val isPosting: Boolean get() = publishingStep != PublishingStep.Idle
    val isRecording: Boolean get() = recordingState == RecordingState.Recording
    val canAddMoreImages: Boolean get() = selectedImageUris.size < MAX_IMAGES
    val selectedCircle: CircleSummary? get() = circles.firstOrNull { it.id == selectedCircleId }
    // A circle post needs the circle's name, so it waits until the circle has loaded
    val isAudienceReady: Boolean get() = selectedCircleId == null || selectedCircle != null
    val canPublish: Boolean get() = content.isNotBlank() && !isPosting && isAudienceReady
}

val MOOD_OPTIONS = listOf(
    "😊 Feliz",
    "🚀 Motivado",
    "🤔 Pensativo",
    "😌 En paz",
    "🎉 Emocionado",
    "😔 Triste",
    "😤 Frustrado",
    "😴 Cansado",
    "🌱 Creativo",
    "❤️ Agradecido",
)

sealed interface CreatePostIntent {
    data class ContentChanged(val value: String) : CreatePostIntent
    // Images
    data class ImagesSelected(val uris: List<Uri>) : CreatePostIntent
    data class ImageRemoved(val index: Int) : CreatePostIntent
    // Audio
    data object StartRecording : CreatePostIntent
    data object StopRecording : CreatePostIntent
    data object RemoveAudio : CreatePostIntent
    // File
    data class FileSelected(val uri: Uri, val name: String) : CreatePostIntent
    data object RemoveFile : CreatePostIntent
    // GIF
    data class ShowGiphyPicker(val kind: GiphyKind) : CreatePostIntent
    data object HideGiphyPicker : CreatePostIntent
    data class GifSelected(val url: String) : CreatePostIntent
    data object RemoveGif : CreatePostIntent
    // Mood
    data object ShowMoodPicker : CreatePostIntent
    data object HideMoodPicker : CreatePostIntent
    data class MoodSelected(val mood: String?) : CreatePostIntent
    // Audience
    data object ShowAudiencePicker : CreatePostIntent
    data object HideAudiencePicker : CreatePostIntent
    data class AudienceSelected(val circleId: String?) : CreatePostIntent
    // Location
    data object RequestLocation : CreatePostIntent
    data class LocationResolved(val name: String) : CreatePostIntent
    data object RemoveLocation : CreatePostIntent
    // Timer tick for recording UI
    data class RecordingTick(val durationMs: Long) : CreatePostIntent
    // Actions
    data object Publish : CreatePostIntent
    data object DismissError : CreatePostIntent
}

sealed interface CreatePostEvent {
    data object PostCreated : CreatePostEvent
    data object NavigateBack : CreatePostEvent
    data object RequestAudioPermission : CreatePostEvent
    data object RequestLocationPermission : CreatePostEvent
}
