package com.nexus.feature.feed.presentation.ui.story

import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 5000L

@OptIn(UnstableApi::class)
@Composable
fun StoryViewerScreen(
    stories: List<Story>,
    initialIndex: Int = 0,
    onSeen: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var progress by remember { mutableFloatStateOf(0f) }
    // isPaused: true por hold o por foco en el campo de respuesta
    var isPausedByHold by remember { mutableStateOf(false) }
    var isReplying by remember { mutableStateOf(false) }
    val isPaused = isPausedByHold || isReplying

    var replyText by remember { mutableStateOf("") }

    val currentStory = stories.getOrNull(currentIndex)

    // Duración real de la historia: se actualiza cuando el video reporta su duración
    var storyDurationMs by remember(currentIndex) {
        mutableLongStateOf(effectiveDuration(currentStory))
    }

    LaunchedEffect(currentIndex) {
        currentStory?.let { onSeen(it.id) }
    }

    // Avance automático — se detiene si isPaused; usa la duración real del media
    LaunchedEffect(currentIndex, isPaused, storyDurationMs) {
        if (isPaused) return@LaunchedEffect
        progress = 0f
        val steps = 100
        val stepDuration = storyDurationMs / steps
        repeat(steps) {
            delay(stepDuration)
            if (!isPaused) progress = (it + 1) / steps.toFloat()
        }
        if (!isPaused) {
            if (currentIndex < stories.lastIndex) currentIndex++
            else onDismiss()
        }
    }

    if (currentStory == null) {
        onDismiss()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(storyBackground(currentStory))
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onPress = {
                        isPausedByHold = true
                        tryAwaitRelease()
                        isPausedByHold = false
                    },
                    onTap = { offset ->
                        // No navegar mientras el usuario escribe
                        if (isReplying) return@detectTapGestures
                        if (offset.x < size.width / 2) {
                            if (currentIndex > 0) currentIndex-- else onDismiss()
                        } else {
                            if (currentIndex < stories.lastIndex) currentIndex++ else onDismiss()
                        }
                        progress = 0f
                    },
                )
            },
    ) {
        StoryContent(
            story = currentStory,
            modifier = Modifier.fillMaxSize(),
            onDurationReady = { durationMs -> storyDurationMs = durationMs },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // Barras de progreso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // Header autor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentStory.authorAvatarUrl != null) {
                        AsyncImage(
                            model = currentStory.authorAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = currentStory.authorName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStory.authorName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = timeAgo(currentStory.createdAt),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                // Indicador visual de pausa al responder
                if (isReplying) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("⏸", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // Chip música con reproducción real
            if (currentStory.musicTitle != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    MusicChip(story = currentStory, isPaused = isPaused)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Barra de respuesta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .onFocusChanged { focusState ->
                            // Pausa la historia cuando el teclado aparece
                            isReplying = focusState.isFocused
                        },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { inner ->
                        if (replyText.isEmpty()) {
                            Text(
                                "Responder a ${currentStory.authorName.split(" ").first()}…",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                            )
                        }
                        inner()
                    },
                )
                if (replyText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6C63FF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                replyText = ""
                                isReplying = false
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun StoryContent(
    story: Story,
    modifier: Modifier = Modifier,
    onDurationReady: (Long) -> Unit = {},
) {
    val context = LocalContext.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            (story.type == StoryType.VIDEO || story.type == StoryType.REEL) && story.mediaUrl != null -> {
                val exoPlayer = remember(story.mediaUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(story.mediaUrl))
                        repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        playWhenReady = true
                        prepare()
                    }
                }
                DisposableEffect(story.mediaUrl) {
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                val dur = exoPlayer.duration
                                if (dur > 0) onDurationReady(dur)
                            }
                        }
                    }
                    exoPlayer.addListener(listener)
                    onDispose {
                        exoPlayer.removeListener(listener)
                        exoPlayer.release()
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            story.type == StoryType.IMAGE && story.mediaUrl != null -> {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> Unit
        }

        if (!story.text.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = if (story.mediaUrl != null) 0.35f else 0f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = story.text,
                    color = Color(story.textColor),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MusicChip(story: Story, isPaused: Boolean) {
    // Todos los hooks deben estar fuera de cualquier condicional (regla de Compose)
    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }
    val hasMusic = !story.musicPreviewUrl.isNullOrBlank()

    val mediaPlayer = remember(story.musicPreviewUrl) {
        if (story.musicPreviewUrl != null) {
            MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(story.musicPreviewUrl)
                setVolume(1f, 1f)
                prepareAsync()
                setOnPreparedListener { mp ->
                    loudnessEnhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                        setTargetGain(800)
                        enabled = true
                    }
                    mp.seekTo(story.musicStartMs.toInt())
                    if (!isPaused) mp.start()
                }
            }
        } else null
    }

    // Pausa/reanuda respetando el punto de inicio elegido
    LaunchedEffect(isPaused) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        if (isPaused) {
            if (mp.isPlaying) mp.pause()
        } else {
            if (!mp.isPlaying) {
                mp.seekTo(story.musicStartMs.toInt())
                mp.start()
            }
        }
    }

    // Detiene la reproducción al acabar la duración elegida
    LaunchedEffect(isPaused) {
        if (!isPaused && mediaPlayer != null) {
            delay(story.musicDurationMs)
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        }
    }

    DisposableEffect(story.musicPreviewUrl) {
        onDispose {
            loudnessEnhancer?.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    if (!hasMusic) return

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Ícono animado de nota musical — quieto si está pausado
        Text(
            text = if (isPaused) "♪" else "♫",
            color = Color.White,
            fontSize = 13.sp,
        )
        Text(
            text = buildString {
                append(story.musicTitle)
                if (!story.musicArtist.isNullOrBlank()) append(" · ${story.musicArtist}")
            },
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun storyBackground(story: Story): Brush =
    Brush.verticalGradient(story.backgroundGradient.colors.map { Color(it) })

private fun effectiveDuration(story: Story?): Long = when {
    story == null -> STORY_DURATION_MS
    story.type == StoryType.VIDEO || story.type == StoryType.REEL -> STORY_DURATION_MS
    story.musicPreviewUrl != null -> story.musicDurationMs
    else -> STORY_DURATION_MS
}

private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "ahora"
        diff < 3_600_000 -> "${diff / 60_000}m"
        else -> "${diff / 3_600_000}h"
    }
}
