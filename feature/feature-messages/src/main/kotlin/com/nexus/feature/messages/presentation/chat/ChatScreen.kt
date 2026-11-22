package com.nexus.feature.messages.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.GifBox
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nexus.core.giphy.GiphyKind
import com.nexus.core.giphy.ui.GiphyPickerSheet
import com.nexus.core.ui.components.NexusEmptyState
import com.nexus.core.ui.components.UserAvatar
import com.nexus.feature.messages.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Spanish = Locale("es", "MX")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    // Messages only count as read while the chat is actually on screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onIntent(ChatIntent.VisibilityChanged(true))
                Lifecycle.Event.ON_PAUSE -> viewModel.onIntent(ChatIntent.VisibilityChanged(false))
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onIntent(ChatIntent.VisibilityChanged(false))
        }
    }

    // reverseLayout: item 0 is the newest message, so a new one lands in view
    val rows = remember(state.messages, state.seenMessageId) { buildRows(state.messages, state.currentUid, state.seenMessageId) }
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = state.otherName, photoUrl = state.otherPhotoUrl, size = 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            state.otherName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (state.canWrite) {
                    InputBar(
                        text = state.input,
                        canSend = state.canSend,
                        isSending = state.isSending,
                        canSendGif = state.canSendGif,
                        onTextChange = { viewModel.onIntent(ChatIntent.InputChanged(it)) },
                        onSend = { viewModel.onIntent(ChatIntent.Send) },
                        onOpenGifs = { viewModel.onIntent(ChatIntent.ShowGiphyPicker(GiphyKind.GIF)) },
                    )
                } else {
                    Text(
                        "Ya no compartes ningún círculo con esta persona, así que no puedes iniciar una conversación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.messages.isEmpty() -> NexusEmptyState(
                    icon = if (state.canWrite) Icons.Outlined.Lock else Icons.Outlined.Groups,
                    title = "Saluda a ${state.otherName.substringBefore(' ')}",
                    message = "Solo ustedes dos pueden leer esta conversación.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(rows.size, key = { rows[it].key }) { index ->
                        when (val row = rows[index]) {
                            is ChatRow.Day -> DaySeparator(row.label)
                            is ChatRow.Message -> Bubble(row)
                        }
                    }
                }
            }
        }
    }

    state.giphyPicker?.let { kind ->
        GiphyPickerSheet(
            initialKind = kind,
            onPicked = { viewModel.onIntent(ChatIntent.SendGif(it)) },
            onDismiss = { viewModel.onIntent(ChatIntent.HideGiphyPicker) },
        )
    }
}

@Composable
private fun InputBar(
    text: String,
    canSend: Boolean,
    isSending: Boolean,
    canSendGif: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenGifs: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        IconButton(onClick = onOpenGifs, enabled = canSendGif, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Outlined.GifBox,
                contentDescription = "GIF o emoji animado",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Escribe un mensaje") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Spacer(Modifier.size(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (isSending) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Bubble(row: ChatRow.Message) {
    val mine = row.isMine
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (mine) 18.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 18.dp,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        row.message.gifUrl?.let { url ->
            // Without a bubble: animated emoji are transparent and read better on the background
            AsyncImage(
                model = url,
                contentDescription = "GIF",
                contentScale = ContentScale.Fit,
                alignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
                modifier = Modifier
                    .size(width = 200.dp, height = 160.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.size(2.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            if (row.message.text.isNotEmpty()) {
                Text(
                    text = row.message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        .copy(alpha = 0.7f),
                )
                if (row.message.isPending) {
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = "Enviando",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                }
            }
        }
        if (row.seen) {
            Text(
                "Visto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

sealed interface ChatRow {
    val key: String

    data class Day(val label: String) : ChatRow {
        override val key = "day-$label"
    }

    data class Message(val message: ChatMessage, val isMine: Boolean, val time: String, val seen: Boolean) : ChatRow {
        override val key = message.id
    }
}

/** Newest first (the list is reversed), with a day label above each day's first message. */
internal fun buildRows(
    messages: List<ChatMessage>,
    currentUid: String?,
    seenMessageId: String?,
    now: Long = System.currentTimeMillis(),
): List<ChatRow> {
    val timeFormat = SimpleDateFormat("HH:mm", Spanish)
    val rows = mutableListOf<ChatRow>()
    var currentDay: String? = null
    messages.forEach { message ->
        val day = dayLabel(message.createdAt, now)
        if (day != currentDay) {
            rows += ChatRow.Day(day)
            currentDay = day
        }
        rows += ChatRow.Message(
            message = message,
            isMine = message.senderId == currentUid,
            time = timeFormat.format(Date(message.createdAt)),
            seen = message.id == seenMessageId,
        )
    }
    return rows.asReversed()
}

internal fun dayLabel(millis: Long, now: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    fun Calendar.sameDay(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        day.sameDay(today) -> "Hoy"
        day.sameDay(yesterday) -> "Ayer"
        day.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> SimpleDateFormat("EEEE d 'de' MMMM", Spanish).format(Date(millis))
        else -> SimpleDateFormat("d MMM yyyy", Spanish).format(Date(millis))
    }
}
