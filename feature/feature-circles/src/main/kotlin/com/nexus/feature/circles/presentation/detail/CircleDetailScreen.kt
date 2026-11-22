package com.nexus.feature.circles.presentation.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.NexusEmptyState
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.presentation.detail.components.CircleHeader
import com.nexus.feature.circles.presentation.detail.components.CirclePostRow
import com.nexus.feature.circles.presentation.detail.components.CircleTabs
import com.nexus.feature.circles.presentation.detail.components.MemberRow
import com.nexus.feature.circles.presentation.detail.components.PendingPostRow
import com.nexus.feature.circles.presentation.detail.components.PostRowSkeleton

private const val ANIMATED_ROWS = 6

/** What the confirmation dialog is asking about. */
private sealed interface Confirm {
    data object Leave : Confirm
    data object Delete : Confirm
    data object Regenerate : Confirm
    data class Remove(val member: CircleMember) : Confirm
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePost: (circleId: String) -> Unit,
    onMessageMember: (uid: String) -> Unit,
    viewModel: CircleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CircleDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is CircleDetailEvent.Closed -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                    currentOnBack()
                }
            }
        }
    }

    val circle = state.circle

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = circle?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (circle != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (state.isOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar círculo", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            confirm = Confirm.Delete
                                        },
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Salir del círculo", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            confirm = Confirm.Leave
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            if (circle == null) {
                if (state.isLoading) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { PostRowSkeleton() }
                    }
                }
                return@Box
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onIntent(CircleDetailIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "header", contentType = "header") {
                        CircleHeader(
                            circle = circle,
                            isOwner = state.isOwner,
                            onCopyCode = {
                                clipboard.setText(AnnotatedString(InviteCode.format(circle.inviteCode)))
                            },
                            onShareCode = { shareInvite(context, circle) },
                            onRegenerateCode = { confirm = Confirm.Regenerate },
                            onCreatePost = { onCreatePost(circle.id) },
                        )
                    }
                    item(key = "tabs", contentType = "tabs") {
                        CircleTabs(
                            selected = state.selectedTab,
                            postsCount = state.posts.size.takeUnless { state.postsLoading },
                            membersCount = circle.memberIds.size,
                            onSelect = { viewModel.onIntent(CircleDetailIntent.SelectTab(it)) },
                        )
                    }
                    when (state.selectedTab) {
                        CircleTab.POSTS -> postsTab(
                            state = state,
                            onPostClick = onPostClick,
                            onCreatePost = { onCreatePost(circle.id) },
                            onRetry = { viewModel.onIntent(CircleDetailIntent.RetryPosts) },
                            onRetryPending = { viewModel.onIntent(CircleDetailIntent.RetryPending(it)) },
                            onDiscardPending = { viewModel.onIntent(CircleDetailIntent.DiscardPending(it)) },
                        )
                        CircleTab.MEMBERS -> membersTab(
                            state = state,
                            onMessage = onMessageMember,
                            onRemove = { confirm = Confirm.Remove(it) },
                        )
                    }
                }
            }
        }
    }

    confirm?.let { current ->
        val name = circle?.name.orEmpty()
        val (title, text, action) = when (current) {
            Confirm.Leave -> Triple(
                "¿Salir de $name?",
                "Dejarás de ver sus publicaciones. Para volver necesitarás un código nuevo.",
                "Salir",
            )
            Confirm.Delete -> Triple(
                "¿Eliminar $name?",
                "El círculo desaparece para todos sus miembros. Sus publicaciones dejarán de verse. No se puede deshacer.",
                "Eliminar",
            )
            Confirm.Regenerate -> Triple(
                "¿Cambiar el código?",
                "El código actual dejará de funcionar. Quien ya está en el círculo no se ve afectado.",
                "Cambiar",
            )
            is Confirm.Remove -> Triple(
                "¿Quitar a ${current.member.name}?",
                "Dejará de ver el círculo. El código de invitación cambiará para que no pueda volver a entrar con él.",
                "Quitar",
            )
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    viewModel.onIntent(
                        when (current) {
                            Confirm.Leave -> CircleDetailIntent.Leave
                            Confirm.Delete -> CircleDetailIntent.Delete
                            Confirm.Regenerate -> CircleDetailIntent.RegenerateCode
                            is Confirm.Remove -> CircleDetailIntent.RemoveMember(current.member.uid)
                        },
                    )
                }) {
                    Text(
                        action,
                        color = if (current == Confirm.Regenerate) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

private fun LazyListScope.postsTab(
    state: CircleDetailState,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onRetry: () -> Unit,
    onRetryPending: (String) -> Unit,
    onDiscardPending: (String) -> Unit,
) {
    items(state.pending, key = { "pending-${it.tempId}" }, contentType = { "pending" }) { pending ->
        PendingPostRow(
            post = pending,
            onRetry = { onRetryPending(pending.tempId) },
            onDiscard = { onDiscardPending(pending.tempId) },
            modifier = Modifier.animateItem(),
        )
    }
    when {
        state.postsLoading && state.posts.isEmpty() -> items(3) { PostRowSkeleton() }
        state.postsError != null && state.posts.isEmpty() -> item(key = "error") {
            NexusEmptyState(
                icon = Icons.Outlined.CloudOff,
                title = state.postsError,
                message = "Revisa tu conexión e inténtalo de nuevo.",
                actionLabel = "Reintentar",
                onAction = onRetry,
            )
        }
        state.posts.isEmpty() && state.pending.isEmpty() -> item(key = "empty") {
            NexusEmptyState(
                icon = Icons.Outlined.EditNote,
                title = "Nadie ha publicado aún",
                message = "Lo que publiques aquí solo lo verán los miembros de este círculo.",
                actionLabel = "Publicar",
                onAction = onCreatePost,
            )
        }
        else -> itemsIndexed(state.posts, key = { _, p -> p.id }, contentType = { _, _ -> "post" }) { index, post ->
            AppearOnce(
                enabled = index < ANIMATED_ROWS,
                delayMillis = index * 45,
                modifier = Modifier.animateItem(),
            ) {
                CirclePostRow(post = post, onClick = { onPostClick(post.id) })
            }
        }
    }
}

private fun LazyListScope.membersTab(
    state: CircleDetailState,
    onMessage: (String) -> Unit,
    onRemove: (CircleMember) -> Unit,
) {
    if (state.membersLoading && state.members.isEmpty()) {
        items(3) { PostRowSkeleton() }
        return
    }
    items(state.members, key = { "member-${it.uid}" }, contentType = { "member" }) { member ->
        MemberRow(
            member = member,
            canRemove = state.isOwner && !member.isOwner,
            onMessage = { onMessage(member.uid) },
            onRemove = { onRemove(member) },
            modifier = Modifier.animateItem(),
        )
    }
    item(key = "members-note") {
        Text(
            text = "Puedes escribirle a cualquier persona de tus círculos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}

private fun shareInvite(context: android.content.Context, circle: CircleSummary) {
    val text = "Únete a mi círculo «${circle.name}» en Nexus. " +
        "Abre Círculos › Unirme y escribe el código ${InviteCode.format(circle.inviteCode)}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Invitar a ${circle.name}"))
}
