package com.nexus.feature.post.presentation.create.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.GifBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nexus.core.giphy.GiphyKind
import com.nexus.core.giphy.ui.GiphyPickerSheet
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.theme.Amber500
import com.nexus.core.ui.theme.Gray900
import com.nexus.feature.post.presentation.create.CreatePostEvent
import com.nexus.feature.post.presentation.create.CreatePostIntent
import com.nexus.feature.post.presentation.create.CreatePostUiState
import com.nexus.feature.post.presentation.create.CreatePostViewModel
import com.nexus.feature.post.presentation.create.MOOD_OPTIONS
import com.nexus.feature.post.presentation.create.RecordingState
import com.nexus.feature.post.presentation.create.ui.components.AudioRecorderPanel
import com.nexus.feature.post.presentation.create.ui.components.AudienceChip
import com.nexus.feature.post.presentation.create.ui.components.AudiencePickerSheet
import com.nexus.feature.post.presentation.create.ui.components.ImageGridPreview
import com.nexus.feature.post.presentation.create.ui.components.MoodPickerSheet

private const val MAX_CONTENT_LENGTH = 500

// Must stay in sync with posts/{postId}/files in storage.rules, or the upload is rejected
private val ATTACHMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/plain",
    "text/csv",
    "application/zip",
    "application/msword",
    "application/vnd.ms-excel",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/vnd.oasis.opendocument.presentation",
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
    "image/heic",
    "image/heif",
    "audio/*",
    "video/*",
)

/**
 * The name the user sees for a picked document. lastPathSegment is a provider id
 * such as "document:1234" or "primary:Download/x.pdf", not the file name.
 */
private fun Context.displayNameOf(uri: Uri): String? = runCatching {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
        }
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit = onNavigateBack,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ── Launchers ────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onIntent(CreatePostIntent.ImagesSelected(uris))
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = context.displayNameOf(uri) ?: "archivo"
            viewModel.onIntent(CreatePostIntent.FileSelected(uri, name))
        }
    }

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onAudioPermissionGranted()
        else viewModel.onAudioPermissionDenied()
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onLocationPermissionGranted()
        else viewModel.onLocationPermissionDenied()
    }

    // ── Events ───────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreatePostEvent.PostCreated -> onPostCreated()
                CreatePostEvent.NavigateBack -> onNavigateBack()
                CreatePostEvent.RequestAudioPermission ->
                    audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                // Only the city is shown, so approximate location is enough. Requesting FINE
                // alone is ignored on Android 12+ (it must come together with COARSE).
                CreatePostEvent.RequestLocationPermission ->
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isPosting) uiState.publishingStep.label else "Nueva publicación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, enabled = !uiState.isPosting) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        PublishButton(
                            enabled = uiState.canPublish,
                            posting = uiState.isPosting,
                            onClick = { viewModel.onIntent(CreatePostIntent.Publish) },
                        )
                    },
                )
                // While uploading, a thin bar under the title says the work is happening
                AnimatedVisibility(visible = uiState.isPosting) {
                    LinearProgressIndicator(
                        color = Amber500,
                        trackColor = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    UserAvatar(
                        name = uiState.currentUserName.ifBlank { "Tú" },
                        photoUrl = uiState.currentUserPhotoUrl,
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentUserName.ifBlank { "Tú" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        AudienceChip(
                            circle = uiState.selectedCircle,
                            isLoadingCircle = !uiState.isAudienceReady,
                            enabled = !uiState.isPosting,
                            onClick = { viewModel.onIntent(CreatePostIntent.ShowAudiencePicker) },
                        )
                    }
                }

                ContextChips(
                    state = uiState,
                    onRemoveMood = { viewModel.onIntent(CreatePostIntent.MoodSelected(null)) },
                    onRemoveLocation = { viewModel.onIntent(CreatePostIntent.RemoveLocation) },
                )

                // Short posts are shown big, like on the feed cards; long ones shrink to read well
                val big = uiState.content.length <= BIG_TEXT_LIMIT
                val textStyle = if (big) {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyLarge
                }
                TextField(
                    value = uiState.content,
                    onValueChange = { viewModel.onIntent(CreatePostIntent.ContentChanged(it)) },
                    placeholder = {
                        Text(
                            text = "¿Qué quieres compartir?",
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .animateContentSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Amber500,
                    ),
                    textStyle = textStyle,
                    enabled = !uiState.isPosting,
                    minLines = 3,
                )

                if (uiState.contentError != null) {
                    Text(
                        text = uiState.contentError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                    )
                }

                if (uiState.selectedImageUris.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    ImageGridPreview(
                        uris = uiState.selectedImageUris,
                        onRemove = { idx -> viewModel.onIntent(CreatePostIntent.ImageRemoved(idx)) },
                    )
                }

                uiState.gifUrl?.let { url ->
                    Spacer(Modifier.height(12.dp))
                    GifPreview(
                        url = url,
                        enabled = !uiState.isPosting,
                        onRemove = { viewModel.onIntent(CreatePostIntent.RemoveGif) },
                    )
                }

                if (uiState.recordingState != RecordingState.Idle) {
                    Spacer(Modifier.height(12.dp))
                    AudioRecorderPanel(
                        recordingState = uiState.recordingState,
                        durationMs = uiState.recordingDurationMs,
                        onStop = { viewModel.onIntent(CreatePostIntent.StopRecording) },
                        onRemove = { viewModel.onIntent(CreatePostIntent.RemoveAudio) },
                    )
                }

                uiState.selectedFileName?.let { name ->
                    Spacer(Modifier.height(12.dp))
                    FileCard(
                        name = name,
                        enabled = !uiState.isPosting,
                        onRemove = { viewModel.onIntent(CreatePostIntent.RemoveFile) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Mood row: one tap sets how the user feels ────────────────
            MoodRow(
                selected = uiState.selectedMood,
                enabled = !uiState.isPosting,
                onSelect = { mood ->
                    viewModel.onIntent(CreatePostIntent.MoodSelected(if (mood == uiState.selectedMood) null else mood))
                },
            )

            // ── Bottom action bar ────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                ToolButton(
                    icon = Icons.Outlined.Image,
                    label = "Fotos",
                    active = uiState.selectedImageUris.isNotEmpty(),
                    enabled = !uiState.isPosting && uiState.canAddMoreImages,
                    onClick = { imagePicker.launch("image/*") },
                )
                ToolButton(
                    icon = Icons.Outlined.Mic,
                    label = "Audio",
                    active = uiState.recordingState != RecordingState.Idle,
                    enabled = !uiState.isPosting && uiState.recordingState == RecordingState.Idle,
                    onClick = { viewModel.onIntent(CreatePostIntent.StartRecording) },
                )
                ToolButton(
                    icon = Icons.Outlined.GifBox,
                    label = "GIF o emoji animado",
                    active = uiState.gifUrl != null,
                    enabled = !uiState.isPosting,
                    onClick = { viewModel.onIntent(CreatePostIntent.ShowGiphyPicker(GiphyKind.GIF)) },
                )
                ToolButton(
                    icon = Icons.Outlined.AttachFile,
                    label = "Archivo",
                    active = uiState.selectedFileUri != null,
                    enabled = !uiState.isPosting && uiState.selectedFileUri == null,
                    onClick = { filePicker.launch(ATTACHMENT_MIME_TYPES) },
                )
                ToolButton(
                    icon = Icons.Outlined.LocationOn,
                    label = "Ubicación",
                    active = uiState.locationName != null,
                    enabled = !uiState.isPosting && !uiState.isLoadingLocation,
                    loading = uiState.isLoadingLocation,
                    onClick = { viewModel.onIntent(CreatePostIntent.RequestLocation) },
                )
                Spacer(Modifier.weight(1f))
                CharacterRing(length = uiState.content.length, max = MAX_CONTENT_LENGTH)
            }
        }
    }

    if (uiState.showAudiencePicker) {
        AudiencePickerSheet(
            circles = uiState.circles,
            selectedCircleId = uiState.selectedCircleId,
            onSelect = { viewModel.onIntent(CreatePostIntent.AudienceSelected(it)) },
            onDismiss = { viewModel.onIntent(CreatePostIntent.HideAudiencePicker) },
        )
    }

    uiState.giphyPicker?.let { kind ->
        GiphyPickerSheet(
            initialKind = kind,
            onPicked = { viewModel.onIntent(CreatePostIntent.GifSelected(it)) },
            onDismiss = { viewModel.onIntent(CreatePostIntent.HideGiphyPicker) },
        )
    }

    if (uiState.showMoodPicker) {
        MoodPickerSheet(
            selectedMood = uiState.selectedMood,
            onMoodSelected = { viewModel.onIntent(CreatePostIntent.MoodSelected(it)) },
            onDismiss = { viewModel.onIntent(CreatePostIntent.HideMoodPicker) },
        )
    }
}

// Up to this length the text is shown in the big headline style
private const val BIG_TEXT_LIMIT = 120

// The ring shows the number only when this many characters are left
private const val COUNTDOWN_FROM = 50

@Composable
private fun PublishButton(enabled: Boolean, posting: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (enabled || posting) Amber500 else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "publishColor",
    )
    val content = if (enabled || posting) Gray900 else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        if (posting) {
            CircularProgressIndicator(color = Gray900, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text("Publicar", fontWeight = FontWeight.Bold, color = content)
        }
    }
}

/** The mood and place the post carries, each removable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextChips(
    state: CreatePostUiState,
    onRemoveMood: () -> Unit,
    onRemoveLocation: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.selectedMood != null || state.locationName != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 60.dp, top = 12.dp),
        ) {
            state.selectedMood?.let { mood ->
                AttachmentChip(label = mood, enabled = !state.isPosting, onRemove = onRemoveMood)
            }
            state.locationName?.let { place ->
                AttachmentChip(label = "📍 $place", enabled = !state.isPosting, onRemove = onRemoveLocation)
            }
        }
    }
}

@Composable
private fun AttachmentChip(label: String, enabled: Boolean, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Amber500.copy(alpha = 0.14f))
            .border(1.dp, Amber500.copy(alpha = 0.35f), CircleShape)
            .padding(start = 12.dp, end = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Amber500,
        )
        IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Quitar $label", modifier = Modifier.size(14.dp), tint = Amber500)
        }
    }
}

/** Every mood as a chip in one scrolling row; the chosen one is amber. */
@Composable
private fun MoodRow(selected: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        MOOD_OPTIONS.forEach { mood ->
            val isSelected = mood == selected
            val background by animateColorAsState(
                if (isSelected) Amber500 else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "moodColor",
            )
            Text(
                text = mood,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Gray900 else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(background)
                    .clickable(enabled = enabled, role = Role.Button) { onSelect(mood) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** A toolbar icon on a soft circle that turns amber while its attachment is in the post. */
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    val background by animateColorAsState(
        if (active) Amber500.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        label = "toolColor",
    )
    val tint = when {
        active -> Amber500
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Amber500, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * How much of the limit is used, as a ring that fills up. The count appears only near the
 * end, turning red when the limit is reached.
 */
@Composable
private fun CharacterRing(length: Int, max: Int) {
    val left = max - length
    val progress by animateFloatAsState((length.toFloat() / max).coerceIn(0f, 1f), label = "chars")
    val color = when {
        left <= 0 -> MaterialTheme.colorScheme.error
        left <= COUNTDOWN_FROM -> Amber500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val grow by animateFloatAsState(if (left <= COUNTDOWN_FROM) 1.15f else 1f, label = "ringGrow")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 4.dp)
            .size(30.dp)
            .scale(grow),
    ) {
        CircularProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeWidth = 3.dp,
            modifier = Modifier.size(26.dp),
        )
        if (left <= COUNTDOWN_FROM) {
            Text(text = left.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FileCard(name: String, enabled: Boolean, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Amber500.copy(alpha = 0.16f)),
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = Amber500, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(Icons.Default.Close, contentDescription = "Quitar archivo", modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** The picked GIF, whole (emoji are transparent), with a button to take it out. */
@Composable
private fun GifPreview(url: String, enabled: Boolean, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = url,
            contentDescription = "GIF elegido",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Quitar GIF", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
