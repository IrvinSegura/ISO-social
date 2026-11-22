package com.nexus.feature.feed.presentation.ui.story

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.media.audiofx.LoudnessEnhancer
import org.json.JSONObject
import java.net.URL
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.model.StoryType
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 5000L
private const val PROGRESS_TICK_MS = 50L

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
    // isPaused: true por hold o mientras la app no está visible
    var isPausedByHold by remember { mutableStateOf(false) }
    var isPausedByLifecycle by remember { mutableStateOf(false) }
    val isPaused = isPausedByHold || isPausedByLifecycle

    // Back closes the viewer instead of leaving the feed or the app
    BackHandler(onBack = onDismiss)

    // In the background, video and music must stop and the story must not keep advancing
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> isPausedByLifecycle = true
                Lifecycle.Event.ON_START -> isPausedByLifecycle = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentStory = stories.getOrNull(currentIndex)

    // Duración real de la historia: se actualiza cuando el video reporta su duración
    var storyDurationMs by remember(currentIndex) {
        mutableLongStateOf(effectiveDuration(currentStory))
    }

    LaunchedEffect(currentIndex) {
        currentStory?.let { onSeen(it.id) }
    }

    // Avance automático — se detiene si isPaused y, al reanudar, sigue desde el progreso
    // actual (progress solo vuelve a 0 al cambiar de historia); usa la duración real del media
    LaunchedEffect(currentIndex, isPaused, storyDurationMs) {
        if (isPaused) return@LaunchedEffect
        val duration = storyDurationMs.coerceAtLeast(1L).toFloat()
        val startProgress = progress
        val startedAt = SystemClock.uptimeMillis()
        while (true) {
            val next = startProgress + (SystemClock.uptimeMillis() - startedAt) / duration
            if (next >= 1f) break
            progress = next
            delay(PROGRESS_TICK_MS)
        }
        progress = 0f
        if (currentIndex < stories.lastIndex) currentIndex++
        else onDismiss()
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
        // A new composition per story, so two videos in a row never share a PlayerView
        // that still points to the previous (released) ExoPlayer
        key(currentStory.id) {
            StoryContent(
                story = currentStory,
                isPaused = isPaused,
                modifier = Modifier.fillMaxSize(),
                onDurationReady = { durationMs -> storyDurationMs = durationMs },
            )
        }

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
                    LinearProgressIndicator(
                        // Read inside the lambda: the 20 ticks per second only redraw the
                        // bar, instead of recomposing the whole viewer and its video
                        progress = {
                            when {
                                index < currentIndex -> 1f
                                index == currentIndex -> progress
                                else -> 0f
                            }
                        },
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

            // The reply bar was removed: there is no messaging backend yet and "Enviar"
            // silently discarded the text. It comes back with the Messages feature.
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun StoryContent(
    story: Story,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    onDurationReady: (Long) -> Unit = {},
) {
    val context = LocalContext.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            (story.type == StoryType.VIDEO || story.type == StoryType.REEL) && story.mediaUrl != null -> {
                val exoPlayer = remember(story.mediaUrl) {
                    StoryMediaCache.newPlayer(context).apply {
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
                // Hold-to-pause and app-in-background must stop the video too, not just the progress
                LaunchedEffect(exoPlayer, isPaused) {
                    exoPlayer.playWhenReady = !isPaused
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            story.type == StoryType.IMAGE && story.mediaUrl != null -> {
                NexusAsyncImage(
                    model = story.mediaUrl,
                    contentDescription = null,
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
    val hasMusic = !story.musicTitle.isNullOrBlank()
    if (!hasMusic) return

    // URL resuelta: puede ser la guardada (iTunes) o buscada en tiempo real si es Deezer/vacía
    var resolvedUrl by remember(story.id) { mutableStateOf<String?>(story.musicPreviewUrl) }

    LaunchedEffect(story.id) {
        val stored = story.musicPreviewUrl
        val isDeezer = stored?.contains("dzcdn.net") == true
        val isEmpty = stored.isNullOrBlank()
        // Si es URL vieja de Deezer o no hay URL, buscar en iTunes con título+artista
        val title = story.musicTitle
        if ((isDeezer || isEmpty) && !title.isNullOrBlank()) {
            // MusicService applies connect/read timeouts, so a stalled lookup cannot hang
            MusicService.resolvePreviewUrl(title, story.musicArtist)?.let { resolvedUrl = it }
        }
    }

    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }
    // seekTo()/start() before onPrepared puts MediaPlayer in error (-38), so playback
    // waits for this flag instead of starting as soon as the player exists
    var isPrepared by remember(story.id, resolvedUrl) { mutableStateOf(false) }
    // Seek to the chosen start only once; resuming after a pause continues where it was
    var hasStarted by remember(story.id, resolvedUrl) { mutableStateOf(false) }

    val mediaPlayer = remember(story.id, resolvedUrl) {
        val url = resolvedUrl ?: return@remember null
        MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)
            setVolume(0f, 0f)
            prepareAsync()
            setOnPreparedListener { mp ->
                loudnessEnhancer = LoudnessEnhancer(mp.audioSessionId).apply {
                    setTargetGain(400)
                    enabled = true
                }
                // The LaunchedEffect below starts playback (with fade-in) once prepared
                isPrepared = true
            }
            // Si la URL falla (expirada), limpiar para que el LaunchedEffect no reintente la misma
            setOnErrorListener { _, _, _ ->
                resolvedUrl = null
                true
            }
        }
    }

    // Pausa/reanuda con fade-in para evitar el "pop"
    LaunchedEffect(isPaused, isPrepared, mediaPlayer) {
        val mp = mediaPlayer ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        if (isPaused) {
            if (mp.isPlaying) mp.pause()
        } else {
            if (!mp.isPlaying) {
                mp.setVolume(0f, 0f)
                if (!hasStarted) {
                    mp.seekTo(story.musicStartMs.toInt())
                    hasStarted = true
                }
                mp.start()
                repeat(20) { i ->
                    delay(8)
                    mp.setVolume((i + 1) / 20f, (i + 1) / 20f)
                }
            }
        }
    }

    // Detiene al acabar la duración elegida
    LaunchedEffect(isPaused, isPrepared, mediaPlayer) {
        if (!isPaused && isPrepared && mediaPlayer != null) {
            delay(story.musicDurationMs)
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            loudnessEnhancer?.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

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
