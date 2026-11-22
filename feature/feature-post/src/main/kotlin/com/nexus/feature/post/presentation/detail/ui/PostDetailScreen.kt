package com.nexus.feature.post.presentation.detail.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.firebase.post.ReportReason
import com.nexus.core.giphy.GiphyKind
import com.nexus.core.giphy.ui.GiphyPickerSheet
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.EcoCelebration
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.core.ui.components.NexusPill
import com.nexus.core.ui.components.ReactionBar
import com.nexus.core.ui.components.ReportDialog
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.components.relativeTimeEs
import com.nexus.core.ui.share.ShareSheet
import com.nexus.core.ui.share.SharedPost
import com.nexus.core.ui.theme.Amber500
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail
import com.nexus.feature.post.presentation.detail.PostDetailEvent
import com.nexus.feature.post.presentation.detail.PostDetailIntent
import com.nexus.feature.post.presentation.detail.PostDetailViewModel
import com.nexus.feature.post.presentation.detail.ui.components.EcoComposer
import com.nexus.feature.post.presentation.detail.ui.components.EcoThreadItem
import com.nexus.feature.post.presentation.detail.ui.components.EditPostSheet

// About one screenful of threads: enough for the cascade on first load
private const val ANIMATED_ITEMS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var draft by rememberSaveable { mutableStateOf("") }
    var showEcoCelebration by remember { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var showShare by rememberSaveable { mutableStateOf(false) }
    // Giphy picker: null while closed, otherwise the tab it opens on
    var giphyKind by remember { mutableStateOf<GiphyKind?>(null) }
    // Threads the user opened past their first replies; kept across rotation
    var expandedThreads by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(PostDetailIntent.SendMediaEco(EcoType.IMAGE, it.toString())) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PostDetailEvent.EcoSent -> {
                    draft = ""
                    showEcoCelebration = true
                }
                is PostDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                PostDetailEvent.EditSaved -> showEditor = false
                PostDetailEvent.Deleted -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(PostDetailIntent.DismissError)
        }
    }

    val post = uiState.post

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (post != null) {
                        // A circle's posts stay inside it, so only public ones can be shared
                        if (post.circleId == null) {
                            IconButton(onClick = { showShare = true }) {
                                Icon(Icons.Outlined.Share, contentDescription = "Compartir")
                            }
                        }
                        val saveTint by animateColorAsState(
                            if (uiState.isSaved) Amber500 else MaterialTheme.colorScheme.onSurface,
                            label = "saveTint",
                        )
                        IconButton(onClick = { viewModel.onIntent(PostDetailIntent.ToggleSave) }) {
                            Icon(
                                imageVector = if (uiState.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (uiState.isSaved) "Quitar de guardados" else "Guardar",
                                tint = saveTint,
                            )
                        }
                        DetailMenu(
                            isOwnPost = post.isOwnPost,
                            onEdit = { showEditor = true },
                            onDelete = { confirmDelete = true },
                            onReport = { showReport = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (post != null) {
                EcoComposer(
                    text = draft,
                    onTextChange = { draft = it },
                    replyingTo = uiState.replyingTo,
                    isSending = uiState.isEcoSending,
                    focusRequester = focusRequester,
                    onSendText = { viewModel.onIntent(PostDetailIntent.SendTextEco(draft.trim())) },
                    onSendEmoji = { viewModel.onIntent(PostDetailIntent.SendEmojiEco(it)) },
                    onPickGif = { giphyKind = GiphyKind.GIF },
                    onPickImage = { imagePicker.launch("image/*") },
                    onCancelReply = { viewModel.onIntent(PostDetailIntent.CancelReply) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.loadFailed && post == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No se pudo cargar la publicación")
                        TextButton(onClick = { viewModel.onIntent(PostDetailIntent.Retry) }) {
                            Text("Reintentar")
                        }
                    }
                }
                post != null -> PostDetailContent(
                    post = post,
                    expandedThreads = expandedThreads,
                    onToggleThread = { id ->
                        expandedThreads = if (id in expandedThreads) expandedThreads - id else expandedThreads + id
                    },
                    onReact = { viewModel.onIntent(PostDetailIntent.React(it)) },
                    onReply = { eco: Eco -> viewModel.onIntent(PostDetailIntent.ReplyTo(eco)) },
                    onLike = { eco: Eco -> viewModel.onIntent(PostDetailIntent.ToggleEcoLike(eco.id)) },
                )
            }
        }
    }

    giphyKind?.let { kind ->
        // A GIF and an animated emoji are both a GIF eco: a Giphy URL
        GiphyPickerSheet(
            initialKind = kind,
            onPicked = { url ->
                giphyKind = null
                viewModel.onIntent(PostDetailIntent.SendMediaEco(EcoType.GIF, url))
            },
            onDismiss = { giphyKind = null },
        )
    }

    if (showShare && post != null && post.circleId == null) {
        ShareSheet(
            post = SharedPost(
                id = post.id,
                content = post.content,
                authorName = post.authorName,
                mood = post.mood,
                imageUrl = post.imageUrls.firstOrNull() ?: post.gifUrl,
            ),
            onDismiss = { showShare = false },
        )
    }

    if (showEditor && post != null) {
        EditPostSheet(
            initialContent = post.content,
            initialMood = post.mood,
            isSaving = uiState.isSavingEdit,
            onSave = { content, mood -> viewModel.onIntent(PostDetailIntent.SaveEdit(content, mood)) },
            onDismiss = { showEditor = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("¿Eliminar publicación?") },
            text = { Text("Se borrará para todos junto con sus fotos y archivos. No se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(PostDetailIntent.Delete) },
                    enabled = !uiState.isDeleting,
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = !uiState.isDeleting) { Text("Cancelar") }
            },
        )
    }

    if (showReport) {
        ReportDialog(
            reasons = ReportReason.entries.map { it.label },
            onDismiss = { showReport = false },
            onSubmit = { index, details ->
                showReport = false
                viewModel.onIntent(PostDetailIntent.Report(ReportReason.entries[index], details))
            },
        )
    }

    EcoCelebration(
        visible = showEcoCelebration,
        onFinished = { showEcoCelebration = false },
    )
}

@Composable
private fun DetailMenu(
    isOwnPost: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isOwnPost) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Reportar") },
                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onReport()
                    },
                )
            }
        }
    }
}

@Composable
private fun PostDetailContent(
    post: PostDetail,
    expandedThreads: List<String>,
    onToggleThread: (String) -> Unit,
    onReact: (String) -> Unit,
    onReply: (Eco) -> Unit,
    onLike: (Eco) -> Unit,
) {
    val threads = remember(post.ecos) { post.threads }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "post") { PostBody(post = post, onReact = onReact) }

        item(key = "ecosHeader") {
            Column {
                HorizontalDivider(
                    thickness = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                ) {
                    Text("Ecos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (post.ecoCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Amber500.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = post.ecoCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Amber500,
                            )
                        }
                    }
                }
            }
        }

        if (threads.isEmpty()) {
            item(key = "empty") { EmptyEcos() }
        } else {
            itemsIndexed(threads, key = { _, thread -> thread.root.id }) { index, thread ->
                AppearOnce(delayMillis = (index * 45).coerceAtMost(200), enabled = index < ANIMATED_ITEMS) {
                    Column {
                        EcoThreadItem(
                            thread = thread,
                            expanded = thread.root.id in expandedThreads,
                            onToggleExpanded = { onToggleThread(thread.root.id) },
                            onReply = onReply,
                            onLike = onLike,
                        )
                        if (index < threads.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }

        item(key = "bottom") { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PostBody(post: PostDetail, onReact: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    add(relativeTimeEs(post.createdAt))
                    post.circleName?.let { add("en $it") }
                    post.locationName?.let { add("📍 $it") }
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        post.mood?.takeIf { it.isNotBlank() }?.let { mood ->
            Spacer(Modifier.height(12.dp))
            NexusPill(text = mood)
        }

        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            ImagePager(urls = post.imageUrls)
        }

        // GIF or animated emoji
        post.gifUrl?.let { url ->
            Spacer(Modifier.height(14.dp))
            NexusAsyncImage(
                model = url,
                contentDescription = "GIF",
                contentScale = ContentScale.Fit,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
        }

        if (post.fileUrl != null && post.fileName != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = Amber500)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = post.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ReactionBar(
            reactions = post.reactions,
            myReaction = post.myReaction,
            onReact = onReact,
        )
        val total = post.reactions.values.sum()
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    total <= 0 -> "Sé el primero en reaccionar"
                    total == 1 -> "1 reacción"
                    else -> "$total reacciones"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (post.ecoCount == 1) "1 eco" else "${post.ecoCount} ecos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImagePager(urls: List<String>) {
    val pagerState = rememberPagerState { urls.size }
    Box {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(18.dp)),
        ) { page ->
            NexusAsyncImage(
                model = urls[page],
                contentDescription = "Imagen ${page + 1} de ${urls.size}",
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (urls.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                repeat(urls.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (selected) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Amber500 else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyEcos() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Amber500.copy(alpha = 0.12f)),
        ) {
            Icon(Icons.Outlined.Forum, contentDescription = null, tint = Amber500)
        }
        Spacer(Modifier.height(12.dp))
        Text("Aún no hay ecos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Los ecos reemplazan los likes: comparte tu perspectiva real.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
