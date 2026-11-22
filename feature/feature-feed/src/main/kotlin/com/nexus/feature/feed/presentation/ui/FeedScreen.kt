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
import com.nexus.core.firebase.post.ReportReason
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.ReportDialog
import com.nexus.core.ui.components.EcoCelebration
import com.nexus.core.ui.components.NexusEmptyState
import com.nexus.core.ui.components.NexusPageHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Spa
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
import androidx.compose.runtime.saveable.rememberSaveable
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

// About one screenful of cards: enough for the cascade on first load
private const val ANIMATED_ITEMS = 6

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
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    // Post being reported; saveable so the dialog survives rotation
    var reportTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showStoryCreator by remember { mutableStateOf(false) }
    var showEcoCelebration by remember { mutableStateOf(false) }
    // Lista del grupo seleccionado + índice de inicio dentro de ese grupo
    var storyViewerData by remember { mutableStateOf<Pair<List<com.nexus.feature.feed.domain.model.Story>, Int>?>(null) }

    // The ViewModel decides whether the feed is old enough to reload (FeedEvent.RefreshFeed)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onIntent(FeedIntent.ScreenResumed)
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
                FeedEvent.RefreshFeed -> posts.refresh()
                is FeedEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
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
                    onRefresh = { viewModel.onIntent(FeedIntent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    ) {
                        item(key = "header") {
                            FeedHeader(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                        }
                        item {
                            StoriesRow(
                                stories = storyUiState.stories,
                                me = storyUiState.me,
                                onAddStory = { showStoryCreator = true },
                                onStoryGroupClick = { group, startIndex ->
                                    storyViewerData = group to startIndex
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp, bottom = 12.dp),
                            )
                        }

                        // Posts optimistas — aparecen al instante, pulsan mientras suben
                        items(
                            count = uiState.pendingPosts.size,
                            key = { index -> uiState.pendingPosts[index].tempId },
                        ) { index ->
                            val pending = uiState.pendingPosts[index]
                            PendingPostCard(
                                post = pending,
                                onRetry = { viewModel.onIntent(FeedIntent.RetryPendingPost(pending.tempId)) },
                                onDismiss = { viewModel.onIntent(FeedIntent.DismissPendingPost(pending.tempId)) },
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
                                // Only the first screenful cascades in; cards reached by scrolling show at once
                                AppearOnce(delayMillis = (index * 45).coerceAtMost(200), enabled = index < ANIMATED_ITEMS) {
                                    PostCard(
                                        post = post,
                                        onEcoClick = { viewModel.onIntent(FeedIntent.OpenEcoSheet(post.id)) },
                                        onClick = { viewModel.onIntent(FeedIntent.NavigateToPost(post.id)) },
                                        onReactionClick = { emoji ->
                                            viewModel.onIntent(FeedIntent.ToggleReaction(post.id, emoji))
                                        },
                                        isSaved = post.id in savedPostIds,
                                        onSaveClick = { viewModel.onIntent(FeedIntent.ToggleSave(post.id)) },
                                        onReportClick = { reportTargetId = post.id },
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

    reportTargetId?.let { postId ->
        ReportDialog(
            reasons = ReportReason.entries.map { it.label },
            onDismiss = { reportTargetId = null },
            onSubmit = { index, details ->
                reportTargetId = null
                viewModel.onIntent(FeedIntent.ReportPost(postId, ReportReason.entries[index], details))
            },
        )
    }

    uiState.ecoTargetPostId?.let { targetPostId ->
        EcoBottomSheet(
            isLoading = uiState.isEcoSending,
            onDismiss = { viewModel.onIntent(FeedIntent.CloseEcoSheet) },
            onSend = { message ->
                viewModel.onIntent(FeedIntent.SendEco(targetPostId, message))
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

/** Same page header as the other tabs. */
@Composable
private fun FeedHeader(modifier: Modifier = Modifier) {
    NexusPageHeader(
        title = "Inicio",
        subtitle = "Lo más reciente de tu comunidad: reacciona o responde con un eco.",
        modifier = modifier,
    )
}

@Composable
private fun FeedEmptyState() {
    NexusEmptyState(
        icon = Icons.Outlined.Spa,
        title = "Aquí nace el diálogo",
        message = "Todavía no hay publicaciones. Comparte una idea, una pregunta o un momento: " +
            "sin métricas, sin ruido.",
    )
}

@Composable
private fun FeedErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NexusEmptyState(
        icon = Icons.Outlined.CloudOff,
        title = "No se pudo cargar el feed",
        message = "Comprueba tu conexión e inténtalo de nuevo.",
        actionLabel = "Reintentar",
        onAction = onRetry,
        modifier = modifier,
    )
}
