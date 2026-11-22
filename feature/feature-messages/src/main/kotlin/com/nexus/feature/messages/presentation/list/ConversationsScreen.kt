package com.nexus.feature.messages.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.nexus.core.ui.components.AppearOnce
import com.nexus.core.ui.components.NexusEmptyState
import com.nexus.core.ui.components.NexusHeaderIconButton
import com.nexus.core.ui.components.NexusOnboardingCard
import com.nexus.core.ui.components.NexusPageHeader
import com.nexus.core.ui.components.NexusStep
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.components.relativeTimeEs
import com.nexus.core.ui.components.shimmer
import com.nexus.core.ui.theme.Amber400
import com.nexus.core.ui.theme.Gray900
import com.nexus.core.ui.theme.NexusAccent
import com.nexus.feature.messages.domain.model.Contact
import com.nexus.feature.messages.domain.model.ConversationItem

private const val ANIMATED_ROWS = 8

@Composable
fun ConversationsScreen(
    onOpenChat: (otherUid: String) -> Unit,
    onGoToCircles: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNew by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
    ) {
        item(key = "header") {
            NexusPageHeader(
                title = "Mensajes",
                subtitle = "Conversaciones uno a uno con la gente de tus círculos.",
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                NexusHeaderIconButton(
                    icon = Icons.Outlined.Edit,
                    contentDescription = "Nuevo mensaje",
                    onClick = { showNew = true },
                    filled = true,
                )
            }
        }
        when {
            state.isLoading -> items(4) {
                ConversationSkeleton()
                Spacer(Modifier.height(8.dp))
            }
            state.error != null && state.items.isEmpty() -> item(key = "error") {
                NexusEmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = state.error.orEmpty(),
                    message = "Revisa tu conexión e inténtalo de nuevo.",
                    actionLabel = "Reintentar",
                    onAction = { viewModel.onIntent(ConversationsIntent.Retry) },
                )
            }
            state.items.isEmpty() -> item(key = "empty") {
                AppearOnce {
                    NexusOnboardingCard(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        title = "Tus conversaciones",
                        message = "Habla uno a uno con la gente de tus círculos, en tiempo real.",
                        steps = listOf(
                            NexusStep("Elige a alguien", "Toca el lápiz y elige a una persona de tus círculos."),
                            NexusStep("Escríbele", "Texto, emojis o un GIF. Verás cuándo lo leyó."),
                            NexusStep("Sin desconocidos", "Solo pueden escribirte personas con las que compartes un círculo."),
                        ),
                        actionLabel = "Escribir un mensaje",
                        actionIcon = Icons.Outlined.Edit,
                        onAction = { showNew = true },
                    )
                }
            }
            else -> {
                item(key = "label") {
                    val unread = state.items.count { it.conversation.unread > 0 }
                    Row(Modifier.padding(start = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recientes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (unread > 0) "  ·  $unread sin leer" else "  ·  ${state.items.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unread > 0) NexusAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(state.items, key = { _, item -> item.conversation.id }) { index, item ->
                    AppearOnce(
                        enabled = index < ANIMATED_ROWS,
                        delayMillis = index * 40,
                        modifier = Modifier.animateItem(),
                    ) {
                        Column {
                            ConversationRow(
                                item = item,
                                isMineLast = item.conversation.lastSenderId == state.currentUid,
                                onClick = { onOpenChat(item.conversation.otherUid) },
                            )
                            if (index < state.items.lastIndex) {
                                // Inset past the avatar, as in a mail list
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        NewMessageSheet(
            contacts = state.contacts,
            isLoading = state.contactsLoading,
            onPick = {
                showNew = false
                onOpenChat(it.uid)
            },
            onGoToCircles = {
                showNew = false
                onGoToCircles()
            },
            onDismiss = { showNew = false },
        )
    }
}

@Composable
private fun ConversationRow(item: ConversationItem, isMineLast: Boolean, onClick: () -> Unit) {
    val unread = item.conversation.unread
    // A plain list row; unread conversations get a soft amber wash
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (unread > 0) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.background
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = item.otherName, photoUrl = item.otherPhotoUrl, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.otherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread > 0) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = relativeTimeEs(item.conversation.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (unread > 0) NexusAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (isMineLast) "Tú: " else "") + item.conversation.lastText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (unread > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        UnreadDot(unread)
                    }
                }
            }
        }
    }
}

@Composable
fun UnreadDot(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            // Amber 400 in both themes with dark text, like every other amber chip
            .background(Amber400)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = Gray900,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessageSheet(
    contacts: List<Contact>,
    isLoading: Boolean,
    onPick: (Contact) -> Unit,
    onGoToCircles: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("¿A quién le escribes?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Personas que están en alguno de tus círculos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            when {
                isLoading -> repeat(3) {
                    ConversationSkeleton()
                    Spacer(Modifier.height(8.dp))
                }
                contacts.isEmpty() -> NexusEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "Aún no hay nadie",
                    message = "Invita a alguien a uno de tus círculos y podrán escribirse.",
                    actionLabel = "Ir a Círculos",
                    onAction = onGoToCircles,
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(contacts, key = { it.uid }) { contact ->
                        Surface(
                            onClick = { onPick(contact) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(name = contact.name, photoUrl = contact.photoUrl, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        contact.circleNames.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConversationSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).shimmer(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.7f).height(12.dp).shimmer())
            }
        }
    }
}
