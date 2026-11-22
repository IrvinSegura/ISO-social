package com.nexus.feature.profile.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.ui.components.AppearOnce
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.presentation.ProfileEvent
import com.nexus.feature.profile.presentation.ProfileIntent
import com.nexus.feature.profile.presentation.ProfileTab
import com.nexus.feature.profile.presentation.ProfileViewModel
import com.nexus.feature.profile.presentation.TabContent
import com.nexus.feature.profile.presentation.ui.components.ProfileEmptyState
import com.nexus.feature.profile.presentation.ui.components.ProfileHeader
import com.nexus.feature.profile.presentation.ui.components.ProfilePostRow
import com.nexus.feature.profile.presentation.ui.components.ProfilePostRowSkeleton
import com.nexus.feature.profile.presentation.ui.components.ProfileTabs
import com.nexus.feature.profile.presentation.ui.components.SettingsSheet

// Only the first rows of a list fade in; the rest scroll in plainly
private const val ANIMATED_ROWS = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is ProfileEvent.Unsaved -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Quitado de guardados",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(ProfileIntent.UndoUnsave(event.postId))
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onIntent(ProfileIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item(key = "header", contentType = "header") {
                    ProfileHeader(
                        user = state.user,
                        stats = state.stats,
                        onSettingsClick = { showSettings = true },
                        // Lines the title up with the other tabs' headers
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                stickyHeader(key = "tabs", contentType = "tabs") {
                    ProfileTabs(
                        selected = state.selectedTab,
                        postsCount = state.myPosts.items.size.takeUnless { state.myPosts.isLoading },
                        savedCount = state.saved.items.size.takeUnless { state.saved.isLoading },
                        onSelect = { viewModel.onIntent(ProfileIntent.SelectTab(it)) },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                when (state.selectedTab) {
                    ProfileTab.POSTS -> tabContent(
                        content = state.myPosts,
                        showAuthor = false,
                        onPostClick = onPostClick,
                        onUnsave = null,
                        empty = {
                            ProfileEmptyState(
                                icon = Icons.Outlined.EditNote,
                                title = "Aún no has publicado",
                                message = "Comparte lo que estás pensando: tus publicaciones aparecerán aquí.",
                                actionLabel = "Crear publicación",
                                onAction = onCreatePost,
                            )
                        },
                        error = { message ->
                            ProfileEmptyState(
                                icon = Icons.Outlined.CloudOff,
                                title = message,
                                message = "Revisa tu conexión e inténtalo de nuevo.",
                                actionLabel = "Reintentar",
                                onAction = { viewModel.onIntent(ProfileIntent.RetryPosts) },
                                isError = true,
                            )
                        },
                    )
                    ProfileTab.SAVED -> tabContent(
                        content = state.saved,
                        showAuthor = true,
                        onPostClick = onPostClick,
                        onUnsave = { viewModel.onIntent(ProfileIntent.Unsave(it)) },
                        empty = {
                            ProfileEmptyState(
                                icon = Icons.Outlined.BookmarkBorder,
                                title = "Nada guardado todavía",
                                message = "Toca ⋮ › Guardar en cualquier publicación del feed para tenerla a mano aquí.",
                            )
                        },
                        error = { message ->
                            ProfileEmptyState(
                                icon = Icons.Outlined.CloudOff,
                                title = message,
                                message = "Revisa tu conexión e inténtalo de nuevo.",
                                actionLabel = "Reintentar",
                                onAction = { viewModel.onIntent(ProfileIntent.RetrySaved) },
                                isError = true,
                            )
                        },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    if (showSettings) {
        SettingsSheet(
            themeMode = state.themeMode,
            onThemeChange = { viewModel.onIntent(ProfileIntent.SetTheme(it)) },
            onSignOut = {
                showSettings = false
                onSignOut()
            },
            onDismiss = { showSettings = false },
        )
    }
}

private fun LazyListScope.tabContent(
    content: TabContent,
    showAuthor: Boolean,
    onPostClick: (String) -> Unit,
    onUnsave: ((String) -> Unit)?,
    empty: @Composable () -> Unit,
    error: @Composable (String) -> Unit,
) {
    when {
        content.isLoading && content.items.isEmpty() -> items(3) {
            ProfilePostRowSkeleton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
        }
        content.error != null && content.items.isEmpty() -> item(key = "error") { error(content.error) }
        content.items.isEmpty() -> item(key = "empty") { empty() }
        else -> itemsIndexed(
            items = content.items,
            key = { _, post: ProfilePost -> post.id },
            contentType = { _, _ -> "post" },
        ) { index, post ->
            AppearOnce(
                enabled = index < ANIMATED_ROWS,
                delayMillis = index * 45,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
            ) {
                ProfilePostRow(
                    post = post,
                    showAuthor = showAuthor,
                    onClick = { onPostClick(post.id) },
                    onUnsave = onUnsave?.let { { it(post.id) } },
                )
            }
        }
    }
}
