package com.nexus.feature.circles.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.core.firebase.circle.CircleDirectory
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.CircleBadge
import com.nexus.core.ui.components.NexusCard
import com.nexus.core.ui.components.NexusEmptyState
import com.nexus.core.ui.components.NexusHeaderIconButton
import com.nexus.core.ui.components.NexusOnboardingCard
import com.nexus.core.ui.components.NexusPageHeader
import com.nexus.core.ui.components.NexusPill
import com.nexus.core.ui.components.NexusStep
import com.nexus.core.ui.components.shimmer
import com.nexus.feature.circles.presentation.list.components.CreateCircleSheet
import com.nexus.feature.circles.presentation.list.components.JoinCircleSheet

private const val ANIMATED_ROWS = 6

@Composable
fun CirclesScreen(
    onOpenCircle: (String) -> Unit,
    viewModel: CirclesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showJoin by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CirclesEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is CirclesEvent.OpenCircle -> {
                    showCreate = false
                    showJoin = false
                    onOpenCircle(event.circleId)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header", contentType = "header") {
                CirclesHeader(
                    // With no circles yet, the onboarding card below holds the actions
                    showActions = state.circles.isNotEmpty(),
                    onCreate = { showCreate = true },
                    onJoin = { showJoin = true },
                )
            }
            when {
                state.isLoading -> items(3) { CircleRowSkeleton() }
                state.error != null && state.circles.isEmpty() -> item(key = "error") {
                    NexusEmptyState(
                        icon = Icons.Outlined.CloudOff,
                        title = state.error.orEmpty(),
                        message = "Revisa tu conexión e inténtalo de nuevo.",
                        actionLabel = "Reintentar",
                        onAction = { viewModel.onIntent(CirclesIntent.Retry) },
                    )
                }
                state.circles.isEmpty() -> item(key = "empty") {
                    AppearOnce {
                        NexusOnboardingCard(
                            icon = Icons.Outlined.Groups,
                            title = "Tu espacio privado",
                            message = "Un círculo es un grupo pequeño, de hasta ${CircleDirectory.MAX_MEMBERS} " +
                                "personas: tu familia, tus amigos o tu equipo.",
                            steps = listOf(
                                NexusStep("Crea un círculo", "Ponle nombre, un ícono y un color."),
                                NexusStep("Comparte el código", "Quien lo tenga puede unirse; tú decides a quién dárselo."),
                                NexusStep("Publica en privado", "Lo que compartas ahí solo lo ven sus miembros, nunca Inicio."),
                            ),
                            actionLabel = "Crear círculo",
                            actionIcon = Icons.Outlined.Add,
                            onAction = { showCreate = true },
                            secondaryLabel = "Tengo un código",
                            secondaryIcon = Icons.Outlined.Key,
                            onSecondary = { showJoin = true },
                        )
                    }
                }
                else -> {
                    item(key = "label", contentType = "label") {
                        SectionLabel(
                            text = "Tus círculos",
                            count = state.circles.size,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    itemsIndexed(state.circles, key = { _, c -> c.id }, contentType = { _, _ -> "circle" }) { index, circle ->
                        AppearOnce(
                            enabled = index < ANIMATED_ROWS,
                            delayMillis = index * 45,
                            modifier = Modifier.animateItem(),
                        ) {
                            CircleRow(
                                circle = circle,
                                isMine = circle.ownerId == state.currentUid,
                                onClick = { onOpenCircle(circle.id) },
                            )
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

    if (showCreate) {
        CreateCircleSheet(
            isCreating = state.isCreating,
            onCreate = { name, emoji, color -> viewModel.onIntent(CirclesIntent.Create(name, emoji, color)) },
            onDismiss = { showCreate = false },
        )
    }
    if (showJoin) {
        JoinCircleSheet(
            join = state.join,
            onLookUp = { viewModel.onIntent(CirclesIntent.LookUpCode(it)) },
            onConfirm = { viewModel.onIntent(CirclesIntent.ConfirmJoin) },
            onReset = { viewModel.onIntent(CirclesIntent.ResetJoin) },
            onDismiss = {
                showJoin = false
                viewModel.onIntent(CirclesIntent.ResetJoin)
            },
        )
    }
}

@Composable
private fun CirclesHeader(showActions: Boolean, onCreate: () -> Unit, onJoin: () -> Unit) {
    NexusPageHeader(
        title = "Círculos",
        subtitle = "Comparte solo con quien tú eliges: familia, amigos, tu equipo.",
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        if (showActions) {
            NexusHeaderIconButton(icon = Icons.Outlined.Key, contentDescription = "Unirme con un código", onClick = onJoin)
            NexusHeaderIconButton(icon = Icons.Outlined.Add, contentDescription = "Crear círculo", onClick = onCreate, filled = true)
        }
    }
}

/** "Tus círculos · 3": a bold label and a muted count above a list. */
@Composable
private fun SectionLabel(text: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "  ·  $count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CircleRow(circle: CircleSummary, isMine: Boolean, onClick: () -> Unit) {
    NexusCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleBadge(emoji = circle.emoji, colorIndex = circle.colorIndex, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val count = circle.memberIds.size
                    Text(
                        text = (if (count == 1) "1 miembro" else "$count miembros") +
                            " de ${CircleDirectory.MAX_MEMBERS}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isMine) NexusPill(text = "Creador", icon = Icons.Outlined.WorkspacePremium)
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CircleRowSkeleton() {
    NexusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer(),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmer(),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmer(),
                )
            }
        }
    }
}
