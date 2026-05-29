package com.nexus.feature.feed.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.EcoCelebration
import com.nexus.core.ui.theme.NexusAccentTeal
import com.nexus.core.ui.theme.NexusPurple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.nexus.feature.feed.presentation.FeedEvent
import com.nexus.feature.feed.presentation.FeedIntent
import com.nexus.feature.feed.presentation.FeedViewModel
import com.nexus.feature.feed.presentation.PendingPostStatus
import com.nexus.feature.feed.presentation.StoryEvent
import com.nexus.feature.feed.presentation.StoryIntent
import com.nexus.feature.feed.presentation.StoryViewModel
import com.nexus.feature.feed.presentation.ui.components.EcoBottomSheet
import com.nexus.feature.feed.presentation.ui.components.FeedSkeleton
import com.nexus.feature.feed.presentation.ui.components.PendingPostCard
import com.nexus.feature.feed.presentation.ui.components.PostCard
import com.nexus.feature.feed.presentation.ui.components.StoriesRow
import com.nexus.feature.feed.presentation.ui.story.StoryCreatorSheet
import com.nexus.feature.feed.presentation.ui.story.StoryViewerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToPost: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storyUiState by storyViewModel.uiState.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showStoryCreator by remember { mutableStateOf(false) }
    var showEcoCelebration by remember { mutableStateOf(false) }
    // Lista del grupo seleccionado + índice de inicio dentro de ese grupo
    var storyViewerData by remember { mutableStateOf<Pair<List<com.nexus.feature.feed.domain.model.Story>, Int>?>(null) }

    // Al volver al feed limpia los pending confirmados y sincroniza con Paging3
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    // Solo refrescamos si no hay pendientes subiendo (ya están en pantalla)
                    if (viewModel.uiState.value.pendingPosts.none {
                            it.status == PendingPostStatus.UPLOADING
                        }
                    ) {
                        viewModel.clearPendingPosts()
                        posts.refresh()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedEvent.NavigateToPost -> onNavigateToPost(event.postId)
                FeedEvent.NavigateToCreatePost -> Unit
                FeedEvent.EcoSent -> showEcoCelebration = true
            }
        }
    }

    // Eventos de historia — siempre activo, no dentro del if(showStoryCreator)
    LaunchedEffect(Unit) {
        storyViewModel.events.collect { event ->
            when (event) {
                is StoryEvent.StoryCreated -> showStoryCreator = false
                is StoryEvent.ShowError -> snackbarHostState.showSnackbar("Error: ${event.message}")
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(FeedIntent.DismissError)
        }
    }

    LaunchedEffect(storyUiState.error) {
        storyUiState.error?.let {
            snackbarHostState.showSnackbar(it)
            storyViewModel.onIntent(StoryIntent.DismissError)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                ),
            ),
    ) {
        when {
            posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0 -> {
                FeedSkeleton(modifier = Modifier.padding(top = 8.dp))
            }

            posts.loadState.refresh is LoadState.Error && posts.itemCount == 0 -> {
                FeedErrorState(
                    onRetry = { posts.retry() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = posts.loadState.refresh is LoadState.Loading,
                    onRefresh = { posts.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    ) {
                        item {
                            StoriesRow(
                                stories = storyUiState.stories,
                                onAddStory = { showStoryCreator = true },
                                onStoryGroupClick = { group, startIndex ->
                                    storyViewerData = group to startIndex
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )
                        }

                        // Posts optimistas — aparecen al instante, pulsan mientras suben
                        items(
                            count = uiState.pendingPosts.size,
                            key = { index -> uiState.pendingPosts[index].tempId },
                        ) { index ->
                            PendingPostCard(
                                post = uiState.pendingPosts[index],
                                onRetry = { /* TODO: reintentar publicación */ },
                                onDismiss = { viewModel.clearPendingPosts() },
                            )
                        }

                        items(
                            count = posts.itemCount,
                            key = { index -> posts.peek(index)?.id ?: index },
                        ) { index ->
                            val post = posts[index]
                            if (post != null) {
                                // Entrada en cascada: las primeras tarjetas ascienden
                                // escalonadas; el retraso se acota para no penalizar
                                // el scroll en posiciones lejanas.
                                AppearOnce(delayMillis = (index * 45).coerceAtMost(200)) {
                                    PostCard(
                                        post = post,
                                        userReaction = uiState.userReactions[post.id],
                                        ecoCountDelta = uiState.ecoCountDeltas[post.id] ?: 0,
                                        onEcoClick = { viewModel.onIntent(FeedIntent.OpenEcoSheet(post.id)) },
                                        onClick = { viewModel.onIntent(FeedIntent.NavigateToPost(post.id)) },
                                        onReactionClick = { emoji ->
                                            viewModel.onIntent(FeedIntent.ToggleReaction(post.id, emoji))
                                        },
                                    )
                                }
                            }
                        }

                        // Feed sin publicaciones (y sin nada subiendo) → invitación, no vacío
                        if (posts.itemCount == 0 &&
                            uiState.pendingPosts.isEmpty() &&
                            posts.loadState.refresh is LoadState.NotLoading &&
                            posts.loadState.append.endOfPaginationReached
                        ) {
                            item { FeedEmptyState() }
                        }

                        if (posts.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    if (uiState.ecoTargetPostId != null) {
        EcoBottomSheet(
            isLoading = uiState.isEcoSending,
            onDismiss = { viewModel.onIntent(FeedIntent.CloseEcoSheet) },
            onSend = { message ->
                viewModel.onIntent(FeedIntent.SendEco(uiState.ecoTargetPostId!!, message))
            },
        )
    }

    if (showStoryCreator) {
        StoryCreatorSheet(
            isCreating = storyUiState.isCreating,
            errorMessage = storyUiState.error,
            onDismiss = {
                storyViewModel.onIntent(StoryIntent.DismissError)
                showStoryCreator = false
            },
            onCreateStory = { type, mediaUri, text, gradient, musicTitle, musicArtist, musicPreviewUrl, musicStartMs, musicDurationMs ->
                storyViewModel.onIntent(
                    StoryIntent.CreateStory(
                        type = type,
                        mediaLocalUri = mediaUri,
                        text = text,
                        gradient = gradient,
                        musicTitle = musicTitle,
                        musicArtist = musicArtist,
                        musicPreviewUrl = musicPreviewUrl,
                        musicStartMs = musicStartMs,
                        musicDurationMs = musicDurationMs,
                    ),
                )
            },
        )
    }

    AnimatedVisibility(
        visible = storyViewerData != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        storyViewerData?.let { (groupStories, startIndex) ->
            StoryViewerScreen(
                stories = groupStories,
                initialIndex = startIndex,
                onSeen = { storyId -> storyViewModel.onIntent(StoryIntent.MarkSeen(storyId)) },
                onDismiss = { storyViewerData = null },
            )
        }
    }

    EcoCelebration(
        visible = showEcoCelebration,
        onFinished = { showEcoCelebration = false },
    )
}

@Composable
private fun FeedEmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Emblema con gradiente de marca
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            NexusPurple.copy(alpha = 0.18f),
                            NexusAccentTeal.copy(alpha = 0.14f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🌱", fontSize = 44.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Aquí nace el diálogo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Todavía no hay publicaciones. Comparte una idea, una pregunta o un momento — sin métricas, sin ruido. Solo lo que de verdad quieres decir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun FeedErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "😕", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No se pudo cargar el feed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Comprueba tu conexión e intenta de nuevo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onRetry) {
            Text("Reintentar", fontWeight = FontWeight.SemiBold)
        }
    }
}
