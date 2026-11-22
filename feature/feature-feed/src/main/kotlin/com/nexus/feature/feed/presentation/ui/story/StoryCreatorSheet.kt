package com.nexus.feature.feed.presentation.ui.story

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.nexus.feature.feed.domain.model.StoryGradient
import com.nexus.feature.feed.domain.model.StoryType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorSheet(
    isCreating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onCreateStory: (
        type: StoryType,
        mediaLocalUri: String?,
        text: String?,
        gradient: StoryGradient,
        musicTitle: String?,
        musicArtist: String?,
        musicPreviewUrl: String?,
        musicStartMs: Long,
        musicDurationMs: Long,
    ) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMode by remember { mutableStateOf(CreatorMode.HISTORY) }
    var storyText by remember { mutableStateOf("") }
    var selectedGradient by remember { mutableStateOf(StoryGradient.PURPLE) }
    var selectedMusicEntry by remember { mutableStateOf<MusicEntry?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    var trimEntry by remember { mutableStateOf<MusicEntry?>(null) }
    // Saveable: the Activity can be recreated while the camera or picker is open
    var selectedMediaUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Which launcher produced selectedMediaUri; the story type comes from this, not from the mode
    var selectedMediaIsVideo by rememberSaveable { mutableStateOf(false) }
    var cameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // ---- Launchers de media ----

    val galleryImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            selectedMediaUri = it
            selectedMediaIsVideo = false
        }
    }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            selectedMediaUri = it
            selectedMediaIsVideo = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            selectedMediaUri = cameraImageUri
            selectedMediaIsVideo = false
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo(),
    ) { success ->
        if (success) {
            selectedMediaUri = cameraImageUri
            selectedMediaIsVideo = true
        }
    }

    // ---- Permisos ----
    // The gallery uses GetContent (system picker), which needs no storage permission.
    // Asking for READ_MEDIA_* blocked the picker when the user granted only partial access.

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createTempFileUri(context, isVideo = false)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val cameraVideoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createTempFileUri(context, isVideo = true)
            cameraImageUri = uri
            cameraVideoLauncher.launch(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Crear",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Selector Historia / Reel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                CreatorMode.entries.forEach { mode ->
                    val selected = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                            )
                            .clickable { selectedMode = mode }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(selectedGradient.colors.map { Color(it) }),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedMediaUri != null) {
                    AsyncImage(
                        model = selectedMediaUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (storyText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedMediaUri != null) Color.Black.copy(alpha = 0.35f)
                                else Color.Transparent,
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = storyText,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else if (selectedMediaUri == null) {
                    Text(
                        text = "Vista previa",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (selectedMusicEntry != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote, null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = selectedMusicEntry!!.displayTitle,
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Selector de gradiente (solo si no hay media)
            if (selectedMediaUri == null) {
                Text(
                    text = "Fondo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(StoryGradient.entries) { gradient ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(gradient.colors.map { Color(it) }))
                                .then(
                                    if (gradient == selectedGradient)
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier,
                                )
                                .clickable { selectedGradient = gradient },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Campo de texto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.TextFields, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                BasicTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (storyText.isEmpty()) {
                            Text(
                                "Escribe algo…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ActionChip(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Cámara",
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = "Imagen",
                    onClick = { galleryImageLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    icon = Icons.Outlined.Videocam,
                    label = "Video",
                    isActive = selectedMode == CreatorMode.REEL,
                    onClick = { galleryVideoLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    icon = Icons.Outlined.MusicNote,
                    label = "Música",
                    isActive = selectedMusicEntry != null,
                    onClick = { showMusicPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Error visible
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "⚠️ $errorMessage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Botón publicar
            Button(
                onClick = {
                    val type = storyTypeFor(selectedMode, selectedMediaUri != null, selectedMediaIsVideo)
                        ?: return@Button
                    onCreateStory(
                        type,
                        selectedMediaUri?.toString(),
                        storyText.ifBlank { null },
                        selectedGradient,
                        selectedMusicEntry?.title,
                        selectedMusicEntry?.artist,
                        selectedMusicEntry?.previewUrl,
                        selectedMusicEntry?.startMs ?: 0L,
                        selectedMusicEntry?.durationMs ?: 30_000L,
                    )
                },
                // A reel stays disabled until a video is picked
                enabled = !isCreating &&
                    storyTypeFor(selectedMode, selectedMediaUri != null, selectedMediaIsVideo) != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Publicando…")
                } else {
                    Text(
                        text = "Publicar ${selectedMode.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showMusicPicker) {
        MusicPickerSheet(
            onDismiss = { showMusicPicker = false },
            onSelect = { entry ->
                showMusicPicker = false
                trimEntry = entry
            },
        )
    }

    trimEntry?.let { entry ->
        MusicTrimSheet(
            entry = entry,
            onDismiss = { trimEntry = null },
            onConfirm = { trimmed ->
                selectedMusicEntry = trimmed
                trimEntry = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (MusicEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MusicEntry>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Búsqueda con debounce de 400ms
    LaunchedEffect(query) {
        searchJob?.cancel()
        if (query.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        searchJob = scope.launch {
            delay(400)
            results = searchMusic(query)
            isSearching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Agregar música",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "La canción se reproducirá en tu historia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // Barra de búsqueda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Search, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Buscar canción o artista…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    },
                )
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (query.isNotBlank() && !isSearching && results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Sin resultados para \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(results) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entry) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (entry.albumArtUrl != null) {
                                AsyncImage(
                                    model = entry.albumArtUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.MusicNote, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                text = entry.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (entry.trackDurationMs > 0) {
                                Text(
                                    text = entry.trackDurationFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (entry.previewUrl.isNullOrBlank()) {
                                Text(
                                    text = "Sin preview",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicTrimSheet(
    entry: MusicEntry,
    onDismiss: () -> Unit,
    onConfirm: (MusicEntry) -> Unit,
) {
    val previewMaxSec = 30f
    val minDurationSec = 3f

    var startSec by remember { mutableStateOf(0f) }
    var endSec by remember { mutableStateOf(15f) }
    var playPositionSec by remember { mutableStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    // Set when the preview URL fails, so the spinner does not spin forever
    var previewFailed by remember { mutableStateOf(false) }
    var playingFull by remember { mutableStateOf(false) }
    // The pending auto-stop of the current play; a new play cancels it so an old timer
    // cannot cut the new playback short
    var stopJob by remember { mutableStateOf<Job?>(null) }
    var volume by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()

    val durationSec = endSec - startSec

    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    val mediaPlayer = remember(entry.previewUrl) {
        if (entry.previewUrl != null) {
            MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(entry.previewUrl)
                setVolume(1f, 1f)
                setOnPreparedListener { mp ->
                    loudnessEnhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                        setTargetGain(400)
                        enabled = true
                    }
                    isPrepared = true
                }
                setOnErrorListener { _, _, _ ->
                    isPrepared = false
                    isPlaying = false
                    previewFailed = true
                    true
                }
                prepareAsync()
            }
        } else null
    }

    DisposableEffect(entry.previewUrl) {
        onDispose {
            loudnessEnhancer?.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(volume) {
        mediaPlayer?.setVolume(volume, volume)
    }

    // Actualiza playPositionSec cada 80ms para animación fluida
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playPositionSec = (mediaPlayer?.currentPosition ?: 0) / 1000f
            delay(80)
        }
    }

    suspend fun fadeIn(mp: MediaPlayer, targetVol: Float = volume) {
        val steps = 20
        repeat(steps) { i ->
            val v = ((i + 1).toFloat() / steps) * targetVol
            mp.setVolume(v, v)
            delay(8)
        }
    }

    fun stopPlayback() {
        stopJob?.cancel()
        stopJob = null
        mediaPlayer?.setVolume(0f, 0f)
        if (isPrepared) mediaPlayer?.pause()
        isPlaying = false
        playingFull = false
    }

    fun play(fromMs: Int, full: Boolean, forMs: Long) {
        mediaPlayer ?: return
        if (!isPrepared) return
        stopJob?.cancel()
        mediaPlayer.setVolume(0f, 0f)
        mediaPlayer.seekTo(fromMs)
        mediaPlayer.start()
        isPlaying = true
        playingFull = full
        stopJob = scope.launch {
            fadeIn(mediaPlayer)
            delay(forMs)
            stopPlayback()
        }
    }

    fun playFull() = play(fromMs = 0, full = true, forMs = previewMaxSec.toLong() * 1000)

    fun playSelection() = play(
        fromMs = (startSec * 1000).toInt(),
        full = false,
        forMs = (durationSec * 1000).roundToLong(),
    )

    ModalBottomSheet(onDismissRequest = {
        stopPlayback()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (entry.albumArtUrl != null) {
                        AsyncImage(
                            model = entry.albumArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = entry.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = "Vista previa de 30s · Arrastra los handles para elegir el fragmento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Reproductor completo ──────────────────────────────────────
            if (entry.previewUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .clickable(enabled = isPrepared) {
                            if (isPlaying && playingFull) stopPlayback() else playFull()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!isPrepared && !previewFailed) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (isPlaying && playingFull) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                previewFailed -> "No se pudo cargar el preview"
                                isPlaying && playingFull -> "${formatSeconds(playPositionSec)} / 30s"
                                else -> "▶  Escuchar preview completo (30s)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Barra de progreso de reproducción completa
                if (isPlaying && playingFull) {
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (playPositionSec / previewMaxSec).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(Modifier.height(20.dp))
            }

            // ── Forma de onda con dos handles arrastrables ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Inicio  ${formatSeconds(startSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Fin  ${formatSeconds(endSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${durationSec.roundToInt()}s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            WaveformTimeline(
                durationSec = previewMaxSec,
                startSec = startSec,
                endSec = endSec,
                minDurationSec = minDurationSec,
                playPositionSec = if (isPlaying) playPositionSec else -1f,
                colorSelected = MaterialTheme.colorScheme.primary,
                colorUnselected = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                colorPlayhead = MaterialTheme.colorScheme.tertiary,
                onRangeChange = { newStart, newEnd ->
                    startSec = newStart
                    endSec = newEnd
                    if (isPlaying && !playingFull) stopPlayback()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("30s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            // ── Botón escuchar selección ──────────────────────────────────
            if (entry.previewUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = isPrepared) {
                            if (isPlaying && !playingFull) stopPlayback() else playSelection()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying && !playingFull) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isPlaying && !playingFull)
                            "Fragmento: ${formatSeconds(playPositionSec - startSec)} / ${durationSec.roundToInt()}s"
                        else
                            "▶  Escuchar solo este fragmento (${durationSec.roundToInt()}s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Control de volumen ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (volume == 0f) "🔇" else if (volume < 0.5f) "🔉" else "🔊",
                    fontSize = 18.sp,
                )
                androidx.compose.material3.Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Info del track + abrir en Spotify ────────────────────────
            if (entry.spotifyUrl != null || entry.trackDurationMs > 0) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1DB954).copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Logo Spotify (círculo verde)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1DB954)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote, null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spotify",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DB954),
                        )
                        if (entry.trackDurationMs > 0) {
                            Text(
                                text = "Duración completa: ${entry.trackDurationFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (entry.spotifyUrl != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1DB954))
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(entry.spotifyUrl),
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Abrir",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    stopPlayback()
                    onConfirm(
                        entry.copy(
                            startMs = (startSec * 1000).roundToLong(),
                            durationMs = (durationSec * 1000).roundToLong(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Usar esta parte",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WaveformTimeline(
    durationSec: Float,
    startSec: Float,
    endSec: Float,
    minDurationSec: Float,
    playPositionSec: Float,
    colorSelected: Color,
    colorUnselected: Color,
    colorPlayhead: Color,
    onRangeChange: (startSec: Float, endSec: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barCount = 60
    val barHeights = remember {
        val rng = java.util.Random(42)
        FloatArray(barCount) { 0.25f + rng.nextFloat() * 0.75f }
    }

    // 0 = ninguno, 1 = handle inicio, 2 = handle fin
    var draggingHandle by remember { mutableIntStateOf(0) }

    val selectedAlpha = animateFloatAsState(
        targetValue = if (playPositionSec >= 0f) 1f else 0.85f,
        label = "alpha",
    )

    Canvas(
        modifier = modifier
            .pointerInput(durationSec, startSec, endSec, minDurationSec) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val startX = (startSec / durationSec) * w
                        val endX = (endSec / durationSec) * w
                        val hitRadius = 28.dp.toPx()
                        draggingHandle = when {
                            kotlin.math.abs(offset.x - startX) < hitRadius &&
                                kotlin.math.abs(offset.x - startX) <= kotlin.math.abs(offset.x - endX) -> 1
                            kotlin.math.abs(offset.x - endX) < hitRadius -> 2
                            else -> 0
                        }
                    },
                    onDrag = { change, _ ->
                        val w = size.width.toFloat()
                        val sec = (change.position.x / w * durationSec).coerceIn(0f, durationSec)
                        when (draggingHandle) {
                            1 -> onRangeChange(
                                sec.coerceIn(0f, endSec - minDurationSec),
                                endSec,
                            )
                            2 -> onRangeChange(
                                startSec,
                                sec.coerceIn(startSec + minDurationSec, durationSec),
                            )
                        }
                    },
                    onDragEnd = { draggingHandle = 0 },
                    onDragCancel = { draggingHandle = 0 },
                )
            },
    ) {
        val w = size.width
        val h = size.height
        val barWidth = w / (barCount * 1.6f)
        val gap = w / barCount
        val handleStroke = 3.dp.toPx()
        val handleRadius = 6.dp.toPx()

        // Barras de la forma de onda
        for (i in 0 until barCount) {
            val barSec = (i.toFloat() / barCount) * durationSec
            val inSelection = barSec >= startSec && barSec < endSec
            val barH = barHeights[i] * h * 0.9f
            val x = i * gap + gap / 2f
            val color = if (inSelection) colorSelected.copy(alpha = selectedAlpha.value)
                        else colorUnselected
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2f, (h - barH) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }

        // Playhead
        if (playPositionSec >= 0f) {
            val px = (playPositionSec / durationSec) * w
            drawLine(
                color = colorPlayhead,
                start = androidx.compose.ui.geometry.Offset(px, 0f),
                end = androidx.compose.ui.geometry.Offset(px, h),
                strokeWidth = handleStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(
                color = colorPlayhead,
                radius = 5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(px, h / 2f),
            )
        }

        // Handle de inicio (línea blanca + círculo arriba)
        val sx = (startSec / durationSec) * w
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(sx, 0f),
            end = androidx.compose.ui.geometry.Offset(sx, h),
            strokeWidth = handleStroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = androidx.compose.ui.geometry.Offset(sx, handleRadius + 2.dp.toPx()),
        )
        drawCircle(
            color = colorSelected,
            radius = handleRadius - 3.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(sx, handleRadius + 2.dp.toPx()),
        )

        // Handle de fin (línea blanca + círculo arriba)
        val ex = (endSec / durationSec) * w
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(ex, 0f),
            end = androidx.compose.ui.geometry.Offset(ex, h),
            strokeWidth = handleStroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = handleRadius,
            center = androidx.compose.ui.geometry.Offset(ex, handleRadius + 2.dp.toPx()),
        )
        drawCircle(
            color = colorSelected,
            radius = handleRadius - 3.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(ex, handleRadius + 2.dp.toPx()),
        )
    }
}

private fun formatSeconds(seconds: Float): String {
    val total = seconds.toInt()
    val m = total / 60
    val s = total % 60
    return if (m > 0) "${m}:${s.toString().padStart(2, '0')}" else "${s}s"
}

private suspend fun searchMusic(query: String): List<MusicEntry> =
    MusicService.searchTracks(query)

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal enum class CreatorMode(val label: String) {
    HISTORY("Historia"),
    REEL("Reel"),
}

/**
 * Story type from the media actually picked. A reel must be a video, so REEL mode
 * without a video returns null (nothing to publish yet).
 */
internal fun storyTypeFor(mode: CreatorMode, hasMedia: Boolean, isVideo: Boolean): StoryType? = when {
    mode == CreatorMode.REEL -> if (hasMedia && isVideo) StoryType.REEL else null
    hasMedia && isVideo -> StoryType.VIDEO
    hasMedia -> StoryType.IMAGE
    else -> StoryType.TEXT
}

data class MusicEntry(
    val title: String,
    val artist: String,
    val previewUrl: String?,
    val albumArtUrl: String? = null,
    val spotifyUrl: String? = null,
    val spotifyUri: String? = null,
    val trackDurationMs: Long = 0L,
    val startMs: Long = 0L,
    val durationMs: Long = 30_000L,
) {
    val displayTitle get() = "$title · $artist"
    val trackDurationFormatted: String get() {
        val totalSec = (trackDurationMs / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }
}

private fun createTempFileUri(context: Context, isVideo: Boolean): Uri {
    val ext = if (isVideo) "mp4" else "jpg"
    val file = File(context.cacheDir, "story_capture_${System.currentTimeMillis()}.$ext")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file,
    )
}
