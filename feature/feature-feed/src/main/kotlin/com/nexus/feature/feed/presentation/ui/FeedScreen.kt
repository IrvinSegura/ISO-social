package com.nexus.feature.feed.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedEvent.NavigateToPost -> onNavigateToPost(event.postId)
                FeedEvent.NavigateToCreatePost -> Unit
                FeedEvent.EcoSent -> snackbarHostState.showSnackbar("Eco enviado")
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(FeedIntent.DismissError)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0 -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            count = posts.itemCount,
                            key = { index -> posts.peek(index)?.id ?: index },
                        ) { index ->
                            val post = posts[index]
                            if (post != null) {
                                PostCard(
                                    post = post,
                                    onEcoClick = { viewModel.onIntent(FeedIntent.OpenEcoSheet(post.id)) },
                                    onClick = { viewModel.onIntent(FeedIntent.NavigateToPost(post.id)) },
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
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
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
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No se pudo cargar el feed",
            style = MaterialTheme.typography.bodyLarge,
        )
        TextButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
