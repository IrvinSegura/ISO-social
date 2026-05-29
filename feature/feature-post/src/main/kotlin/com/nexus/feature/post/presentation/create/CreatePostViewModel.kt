package com.nexus.feature.post.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.NewPostPayload
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.post.domain.usecase.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val MAX_CONTENT_LENGTH = 500
private const val MIN_CONTENT_LENGTH = 10

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val auth: FirebaseAuth,
    private val audioRecorderManager: AudioRecorderManager,
    private val locationHelper: LocationHelper,
    private val newPostEventBus: NewPostEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreatePostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var recordingTimerJob: Job? = null

    init {
        val user = auth.currentUser
        _uiState.update {
            it.copy(
                currentUserName = user?.displayName.orEmpty(),
                currentUserPhotoUrl = user?.photoUrl?.toString(),
            )
        }
    }

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            is CreatePostIntent.ContentChanged ->
                _uiState.update {
                    it.copy(
                        content = intent.value.take(MAX_CONTENT_LENGTH),
                        contentError = null,
                    )
                }

            is CreatePostIntent.ImagesSelected ->
                _uiState.update { state ->
                    val combined = (state.selectedImageUris + intent.uris).take(4)
                    state.copy(selectedImageUris = combined)
                }

            is CreatePostIntent.ImageRemoved ->
                _uiState.update { state ->
                    state.copy(
                        selectedImageUris = state.selectedImageUris.toMutableList().apply {
                            removeAt(intent.index)
                        },
                    )
                }

            CreatePostIntent.StartRecording -> startRecording()
            CreatePostIntent.StopRecording -> stopRecording()
            CreatePostIntent.RemoveAudio -> removeAudio()

            is CreatePostIntent.FileSelected ->
                _uiState.update {
                    it.copy(selectedFileUri = intent.uri, selectedFileName = intent.name)
                }

            CreatePostIntent.RemoveFile ->
                _uiState.update { it.copy(selectedFileUri = null, selectedFileName = null) }

            CreatePostIntent.ShowMoodPicker ->
                _uiState.update { it.copy(showMoodPicker = true) }

            CreatePostIntent.HideMoodPicker ->
                _uiState.update { it.copy(showMoodPicker = false) }

            is CreatePostIntent.MoodSelected ->
                _uiState.update { it.copy(selectedMood = intent.mood, showMoodPicker = false) }

            CreatePostIntent.RequestLocation -> fetchLocation()

            is CreatePostIntent.LocationResolved ->
                _uiState.update { it.copy(locationName = intent.name, isLoadingLocation = false) }

            CreatePostIntent.RemoveLocation ->
                _uiState.update { it.copy(locationName = null) }

            is CreatePostIntent.RecordingTick ->
                _uiState.update { it.copy(recordingDurationMs = intent.durationMs) }

            CreatePostIntent.Publish -> publish()
            CreatePostIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            _events.send(CreatePostEvent.RequestAudioPermission)
        }
        // Actual recording starts from screen after permission is granted
    }

    fun onAudioPermissionGranted() {
        try {
            audioRecorderManager.startRecording()
            _uiState.update { it.copy(recordingState = RecordingState.Recording, recordingDurationMs = 0L) }
            recordingTimerJob = viewModelScope.launch {
                val startMs = System.currentTimeMillis()
                while (true) {
                    delay(100)
                    onIntent(CreatePostIntent.RecordingTick(System.currentTimeMillis() - startMs))
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "No se pudo iniciar la grabación") }
        }
    }

    private fun stopRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val path = audioRecorderManager.stopRecording()
        _uiState.update {
            it.copy(
                recordingState = if (path != null) RecordingState.Recorded else RecordingState.Idle,
                recordedAudioPath = path,
            )
        }
    }

    private fun removeAudio() {
        _uiState.value.recordedAudioPath?.let { audioRecorderManager.deleteFile(it) }
        _uiState.update {
            it.copy(
                recordingState = RecordingState.Idle,
                recordedAudioPath = null,
                recordingDurationMs = 0L,
            )
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _events.send(CreatePostEvent.RequestLocationPermission)
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true) }
            val name = locationHelper.getCurrentCityName()
            _uiState.update {
                it.copy(
                    locationName = name,
                    isLoadingLocation = false,
                    error = if (name == null) "No se pudo obtener la ubicación" else null,
                )
            }
        }
    }

    private fun publish() {
        val state = _uiState.value
        val content = state.content.trim()

        if (content.length < MIN_CONTENT_LENGTH) {
            _uiState.update {
                it.copy(contentError = "Añade un poco más de contexto (mínimo $MIN_CONTENT_LENGTH caracteres).")
            }
            return
        }

        val step = when {
            state.selectedImageUris.isNotEmpty() -> PublishingStep.UploadingImages
            state.recordedAudioPath != null -> PublishingStep.UploadingAudio
            state.selectedFileUri != null -> PublishingStep.UploadingFile
            else -> PublishingStep.SavingPost
        }

        val tempId = "pending_${UUID.randomUUID()}"
        val user = auth.currentUser

        viewModelScope.launch {
            _uiState.update { it.copy(publishingStep = step, contentError = null) }

            // Emitir el post optimista ANTES de subir — aparece en el feed al instante
            newPostEventBus.emitPending(
                NewPostPayload(
                    tempId = tempId,
                    authorId = user?.uid.orEmpty(),
                    authorName = user?.displayName.orEmpty(),
                    authorPhotoUrl = user?.photoUrl?.toString(),
                    content = content,
                    mood = state.selectedMood,
                    locationName = state.locationName,
                    createdAt = System.currentTimeMillis(),
                    isPending = true,
                )
            )

            // Navegar de vuelta inmediatamente, el upload sigue en background
            _events.send(CreatePostEvent.PostCreated)
            _uiState.update { it.copy(publishingStep = PublishingStep.Idle) }

            val result = createPostUseCase(
                content = content,
                circleId = null,
                circleName = null,
                imageUris = state.selectedImageUris,
                audioPath = state.recordedAudioPath,
                fileUri = state.selectedFileUri,
                fileName = state.selectedFileName,
                mood = state.selectedMood,
                locationName = state.locationName,
            )

            when (result) {
                is NexusResult.Success -> {
                    // Confirmar: quita el pendiente y el feed ya tiene el post real de Firestore
                    newPostEventBus.emitConfirmed(tempId, result.data)
                }
                is NexusResult.Error -> {
                    // Marcar como fallido para que el usuario pueda reintentar
                    newPostEventBus.emitFailed(tempId)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorderManager.release()
    }
}
