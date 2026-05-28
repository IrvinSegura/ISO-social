package com.nexus.feature.feed.presentation.ui

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
import androidx.compose.runtime.remember
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
import com.nexus.feature.feed.presentation.ui.components.EcoBottomSheet
import com.nexus.feature.feed.presentation.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToPost: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refrescar feed al volver de otra pantalla (ej. crear post)
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    posts.refresh()
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
                FeedEvent.EcoSent -> snackbarHostState.showSnackbar("¡Eco publicado!")
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(FeedIntent.DismissError)
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
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
                        items(
                            count = posts.itemCount,
                            key = { index -> posts.peek(index)?.id ?: index },
                        ) { index ->
                            val post = posts[index]
                            if (post != null) {
                                PostCard(
                                    post = post,
                                    userReaction = uiState.userReactions[post.id],
                                    onEcoClick = { viewModel.onIntent(FeedIntent.OpenEcoSheet(post.id)) },
                                    onClick = { viewModel.onIntent(FeedIntent.NavigateToPost(post.id)) },
                                    onReactionClick = { emoji ->
                                        viewModel.onIntent(FeedIntent.ToggleReaction(post.id, emoji))
                                    },
                                )
                            }
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
