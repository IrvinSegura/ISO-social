package com.nexus.feature.post.presentation.detail.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nexus.core.ui.components.NexusButton
import com.nexus.feature.post.domain.model.EcoType
import com.nexus.feature.post.domain.model.PostDetail
import com.nexus.feature.post.presentation.create.ui.components.GifPickerSheet
import com.nexus.feature.post.presentation.detail.PostDetailEvent
import com.nexus.feature.post.presentation.detail.PostDetailIntent
import com.nexus.feature.post.presentation.detail.PostDetailViewModel
import com.nexus.feature.post.presentation.detail.ui.components.EcoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_EMOJIS = listOf("❤️", "🔥", "🤔", "💡", "🌱", "😂", "👏", "🙌")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(PostDetailIntent.SendMediaEco(EcoType.IMAGE, it.toString())) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PostDetailEvent.EcoSent -> snackbarHostState.showSnackbar("Eco enviado")
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(PostDetailIntent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(Modifier.width(16.dp))
                ExtendedFloatingActionButton(
                    text = { Text("Responder con Eco") },
                    icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                    onClick = { viewModel.onIntent(PostDetailIntent.OpenEcoSheet) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                uiState.error != null && uiState.post == null -> {
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
                uiState.post != null -> PostDetailContent(post = uiState.post!!)
            }
        }
    }

    // ── Rich Eco Sheet ───────────────────────────────────────────────────
    if (uiState.ecoSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // GIF picker sub-sheet
        var showGifPicker by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(PostDetailIntent.CloseEcoSheet) },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Tab row for eco type
                val tabs = listOf("💬 Texto", "😊 Emoji", "GIF", "🖼 Imagen")
                val currentIndex = when (uiState.selectedEcoType) {
                    EcoType.TEXT -> 0
                    EcoType.EMOJI -> 1
                    EcoType.GIF -> 2
                    EcoType.IMAGE -> 3
                    EcoType.AUDIO -> 0
                }
                val ecoTypes = listOf(EcoType.TEXT, EcoType.EMOJI, EcoType.GIF, EcoType.IMAGE)

                ScrollableTabRow(selectedTabIndex = currentIndex, edgePadding = 16.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = index == currentIndex,
                            onClick = { viewModel.onIntent(PostDetailIntent.SelectEcoType(ecoTypes[index])) },
                            text = { Text(title) },
                        )
                    }
                }

                when (uiState.selectedEcoType) {
                    EcoType.TEXT -> TextEcoPanel(
                        isLoading = uiState.isEcoSending,
                        onSend = { viewModel.onIntent(PostDetailIntent.SendTextEco(it)) },
                    )
                    EcoType.EMOJI -> EmojiEcoPanel(
                        isLoading = uiState.isEcoSending,
                        onEmojiSelected = { viewModel.onIntent(PostDetailIntent.SendEmojiEco(it)) },
                    )
                    EcoType.GIF -> {
                        GifEcoPanel(
                            onOpenPicker = { showGifPicker = true },
                        )
                    }
                    EcoType.IMAGE -> ImageEcoPanel(
                        onPickImage = { imagePicker.launch("image/*") },
                    )
                    EcoType.AUDIO -> Unit
                }
            }
        }

        if (showGifPicker) {
            GifPickerSheet(
                gifs = uiState.gifResults,
                isLoading = uiState.isSearchingGifs,
                onSearch = { viewModel.onIntent(PostDetailIntent.SearchGifs(it)) },
                onGifSelected = { url ->
                    showGifPicker = false
                    viewModel.onIntent(PostDetailIntent.SendMediaEco(EcoType.GIF, url))
                },
                onDismiss = { showGifPicker = false },
            )
        }
    }
}

@Composable
private fun TextEcoPanel(isLoading: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text("Tu perspectiva", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Los ecos reemplazan los likes. Comparte tu punto de vista real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("¿Qué piensas al respecto?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            enabled = !isLoading,
        )
        Spacer(Modifier.height(16.dp))
        NexusButton(
            text = "Enviar eco",
            onClick = { onSend(text) },
            isLoading = isLoading,
            enabled = text.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmojiEcoPanel(isLoading: Boolean, onEmojiSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text("Reacciona con un emoji", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_EMOJIS.take(4).forEach { emoji ->
                EmojiButton(
                    emoji = emoji,
                    isLoading = isLoading,
                    onClick = { onEmojiSelected(emoji) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_EMOJIS.drop(4).forEach { emoji ->
                EmojiButton(
                    emoji = emoji,
                    isLoading = isLoading,
                    onClick = { onEmojiSelected(emoji) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmojiButton(
    emoji: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Text(text = emoji, fontSize = 28.sp)
        }
    }
}

@Composable
private fun GifEcoPanel(onOpenPicker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Reacciona con un GIF", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        NexusButton(
            text = "Buscar GIF",
            onClick = onOpenPicker,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ImageEcoPanel(onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Reacciona con una imagen", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        NexusButton(
            text = "Elegir de galería",
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailContent(post: PostDetail) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Author + date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatFullDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Mood + location
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (post.circleName != null) {
                        Text(
                            text = "en ${post.circleName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (post.mood != null) {
                        Text(
                            text = post.mood,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (post.locationName != null) {
                        Text(
                            text = "📍 ${post.locationName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                )

                // Images carousel
                if (post.imageUrls.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    val pagerState = rememberPagerState { post.imageUrls.size }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    ) { page ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.imageUrls[page])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Imagen ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // File attachment
                if (post.fileUrl != null && post.fileName != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📎", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = post.fileName,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                // Reactions row
                if (post.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        post.reactions.entries
                            .sortedByDescending { it.value }
                            .take(5)
                            .forEach { (emoji, count) ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(text = emoji, fontSize = 14.sp)
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${post.ecoCount} ecos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (post.ecos.isEmpty()) {
            item {
                Text(
                    text = "Sé el primero en responder con tu perspectiva.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            items(post.ecos, key = { it.id }) { eco ->
                EcoItem(eco = eco)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

private fun formatFullDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es")).format(Date(timestamp))
