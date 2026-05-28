package com.nexus.feature.post.presentation.create.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nexus.feature.post.presentation.create.CreatePostEvent
import com.nexus.feature.post.presentation.create.CreatePostIntent
import com.nexus.feature.post.presentation.create.CreatePostViewModel
import com.nexus.feature.post.presentation.create.PublishingStep
import com.nexus.feature.post.presentation.create.RecordingState
import com.nexus.feature.post.presentation.create.ui.components.AudioRecorderPanel
import com.nexus.feature.post.presentation.create.ui.components.ImageGridPreview
import com.nexus.feature.post.presentation.create.ui.components.MoodPickerSheet

private const val MAX_CONTENT_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit = onNavigateBack,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Launchers ────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onIntent(CreatePostIntent.ImagesSelected(uris))
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "archivo"
            viewModel.onIntent(CreatePostIntent.FileSelected(uri, name))
        }
    }

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onAudioPermissionGranted()
        else viewModel.onIntent(CreatePostIntent.DismissError)
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onLocationPermissionGranted()
    }

    // ── Events ───────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreatePostEvent.PostCreated -> onPostCreated()
                CreatePostEvent.NavigateBack -> onNavigateBack()
                CreatePostEvent.RequestAudioPermission ->
                    audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                CreatePostEvent.RequestLocationPermission ->
                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(CreatePostIntent.DismissError)
        }
    }

    // ── Scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isPosting) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (uiState.isPosting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.publishingStep.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onIntent(CreatePostIntent.Publish) },
                            enabled = uiState.canPublish,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text("Publicar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            // ── Content area ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // Author row
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    PostAuthorAvatar(
                        photoUrl = uiState.currentUserPhotoUrl,
                        name = uiState.currentUserName,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentUserName.ifBlank { "Tú" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Mood + Location chips
                        if (uiState.selectedMood != null || uiState.locationName != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (uiState.selectedMood != null) {
                                    AttachmentChip(
                                        label = uiState.selectedMood!!,
                                        onRemove = { viewModel.onIntent(CreatePostIntent.MoodSelected(null)) },
                                    )
                                }
                                if (uiState.locationName != null) {
                                    Spacer(Modifier.width(6.dp))
                                    AttachmentChip(
                                        label = "📍 ${uiState.locationName}",
                                        onRemove = { viewModel.onIntent(CreatePostIntent.RemoveLocation) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Content field
                        TextField(
                            value = uiState.content,
                            onValueChange = { viewModel.onIntent(CreatePostIntent.ContentChanged(it)) },
                            placeholder = {
                                Text(
                                    text = "¿Qué quieres compartir?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            enabled = !uiState.isPosting,
                            minLines = 4,
                        )

                        // Content error
                        if (uiState.contentError != null) {
                            Text(
                                text = uiState.contentError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }

                        // Image grid
                        if (uiState.selectedImageUris.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            ImageGridPreview(
                                uris = uiState.selectedImageUris,
                                onRemove = { idx -> viewModel.onIntent(CreatePostIntent.ImageRemoved(idx)) },
                            )
                        }

                        // Audio recorder panel
                        if (uiState.recordingState != RecordingState.Idle) {
                            Spacer(Modifier.height(12.dp))
                            AudioRecorderPanel(
                                recordingState = uiState.recordingState,
                                durationMs = uiState.recordingDurationMs,
                                onStop = { viewModel.onIntent(CreatePostIntent.StopRecording) },
                                onRemove = { viewModel.onIntent(CreatePostIntent.RemoveAudio) },
                            )
                        }

                        // File chip
                        if (uiState.selectedFileName != null) {
                            Spacer(Modifier.height(12.dp))
                            AttachmentChip(
                                label = "📎 ${uiState.selectedFileName}",
                                onRemove = { viewModel.onIntent(CreatePostIntent.RemoveFile) },
                            )
                        }
                    }
                }
            }

            // ── Bottom action bar ────────────────────────────────────────
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                // Image picker
                IconButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !uiState.isPosting && uiState.canAddMoreImages,
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = "Imágenes",
                        tint = if (uiState.selectedImageUris.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Audio recorder trigger
                IconButton(
                    onClick = { viewModel.onIntent(CreatePostIntent.StartRecording) },
                    enabled = !uiState.isPosting && uiState.recordingState == RecordingState.Idle,
                ) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Audio",
                        tint = if (uiState.recordedAudioPath != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // File picker
                IconButton(
                    onClick = { filePicker.launch("*/*") },
                    enabled = !uiState.isPosting && uiState.selectedFileUri == null,
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = "Archivo",
                        tint = if (uiState.selectedFileUri != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Mood picker
                IconButton(
                    onClick = { viewModel.onIntent(CreatePostIntent.ShowMoodPicker) },
                    enabled = !uiState.isPosting,
                ) {
                    Icon(
                        Icons.Outlined.SentimentSatisfied,
                        contentDescription = "Ánimo",
                        tint = if (uiState.selectedMood != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Location
                IconButton(
                    onClick = { viewModel.onIntent(CreatePostIntent.RequestLocation) },
                    enabled = !uiState.isPosting && !uiState.isLoadingLocation,
                ) {
                    if (uiState.isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = "Ubicación",
                            tint = if (uiState.locationName != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Character counter
                Text(
                    text = "${uiState.content.length}/$MAX_CONTENT_LENGTH",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.content.length >= MAX_CONTENT_LENGTH)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }

    // ── Mood picker sheet ────────────────────────────────────────────────
    if (uiState.showMoodPicker) {
        MoodPickerSheet(
            selectedMood = uiState.selectedMood,
            onMoodSelected = { viewModel.onIntent(CreatePostIntent.MoodSelected(it)) },
            onDismiss = { viewModel.onIntent(CreatePostIntent.HideMoodPicker) },
        )
    }
}

@Composable
private fun AttachmentChip(
    label: String,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PostAuthorAvatar(photoUrl: String?, name: String) {
    if (photoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl).crossfade(true).build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
