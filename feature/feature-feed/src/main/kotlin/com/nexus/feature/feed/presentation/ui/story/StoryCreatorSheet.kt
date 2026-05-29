package com.nexus.feature.feed.presentation.ui.story

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
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
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // ---- Launchers de media ----

    val galleryImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { selectedMediaUri = it } }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { selectedMediaUri = it } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> if (success) selectedMediaUri = cameraImageUri }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo(),
    ) { success -> if (success) selectedMediaUri = cameraImageUri }

    // ---- Permisos ----

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) galleryImageLauncher.launch("image/*") }

    val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_VIDEO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) galleryVideoLauncher.launch("video/*") }

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
                    onClick = { mediaPermissionLauncher.launch(mediaPermission) },
                    modifier = Modifier.weight(1f),
                )
                ActionChip(
                    icon = Icons.Outlined.Videocam,
                    label = "Video",
                    isActive = selectedMode == CreatorMode.REEL,
                    onClick = { videoPermissionLauncher.launch(videoPermission) },
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
                    val type = when {
                        selectedMode == CreatorMode.REEL -> StoryType.REEL
                        selectedMediaUri != null -> StoryType.IMAGE
                        else -> StoryType.TEXT
                    }
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
                enabled = !isCreating,
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
            results = searchSpotify(query)
            isSearching = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
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
    var playingFull by remember { mutableStateOf(false) }
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
                    // Boost ~8 dB para igualar el nivel percibido con otras apps
                    loudnessEnhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                        setTargetGain(800)
                        enabled = true
                    }
                    isPrepared = true
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

    fun stopPlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        playingFull = false
    }

    fun playFull() {
        mediaPlayer ?: return
        if (!isPrepared) return
        mediaPlayer.seekTo(0)
        mediaPlayer.start()
        isPlaying = true
        playingFull = true
        scope.launch {
            delay(previewMaxSec.toLong() * 1000)
            if (isPlaying && playingFull) stopPlayback()
        }
    }

    fun playSelection() {
        mediaPlayer ?: return
        if (!isPrepared) return
        mediaPlayer.seekTo((startSec * 1000).toInt())
        mediaPlayer.start()
        isPlaying = true
        playingFull = false
        scope.launch {
            delay((durationSec * 1000).roundToLong())
            if (isPlaying && !playingFull) stopPlayback()
        }
    }

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
                    if (!isPrepared) {
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
                            text = if (isPlaying && playingFull) "${formatSeconds(playPositionSec)} / 30s"
                            else "▶  Escuchar preview completo (30s)",
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

private suspend fun searchSpotify(query: String): List<MusicEntry> =
    SpotifyService.searchTracks(query)

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

private enum class CreatorMode(val label: String) {
    HISTORY("Historia"),
    REEL("Reel"),
}

data class MusicEntry(
    val title: String,
    val artist: String,
    val previewUrl: String?,
    val albumArtUrl: String? = null,
    val startMs: Long = 0L,
    val durationMs: Long = 30_000L,
) {
    val displayTitle get() = "$title · $artist"
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
