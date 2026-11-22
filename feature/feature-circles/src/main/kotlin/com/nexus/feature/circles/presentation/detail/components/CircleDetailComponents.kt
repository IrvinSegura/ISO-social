package com.nexus.feature.circles.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.ui.components.CircleBadge
import com.nexus.core.ui.components.NexusCard
import com.nexus.core.ui.components.NexusPill
import com.nexus.core.ui.components.NexusPostCard
import com.nexus.core.ui.components.NexusPostStat
import com.nexus.core.ui.components.NexusPrimaryButton
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.components.shimmer
import com.nexus.core.ui.theme.NexusAccent
import com.nexus.core.ui.theme.circleColor
import com.nexus.feature.circles.domain.model.CircleMember
import com.nexus.feature.circles.domain.model.CirclePost
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.presentation.detail.CircleTab
import com.nexus.feature.circles.presentation.detail.PendingCirclePost

/** Badge, name, invite code and the "publish here" action. */
@Composable
fun CircleHeader(
    circle: CircleSummary,
    isOwner: Boolean,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
    onRegenerateCode: () -> Unit,
    onCreatePost: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleBadge(emoji = circle.emoji, colorIndex = circle.colorIndex, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val count = circle.memberIds.size
                Text(
                    text = if (count == 1) "Solo tú, por ahora" else "$count miembros",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        NexusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            Text(
                text = "Código de invitación",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = InviteCode.format(circle.inviteCode),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                    ),
                    color = circleColor(circle.colorIndex),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCopyCode) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar código")
                }
                IconButton(onClick = onShareCode) {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir invitación")
                }
            }
            if (isOwner) {
                TextButton(onClick = onRegenerateCode, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cambiar código", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        NexusPrimaryButton(
            text = "Publicar en el círculo",
            onClick = onCreatePost,
            icon = Icons.Outlined.Edit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        )
    }
}

/** Segmented control in the blog's style: a gray track with the selected option raised. */
@Composable
fun CircleTabs(
    selected: CircleTab,
    postsCount: Int?,
    membersCount: Int,
    onSelect: (CircleTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        TabOption("Publicaciones", postsCount, selected == CircleTab.POSTS, Modifier.weight(1f)) { onSelect(CircleTab.POSTS) }
        TabOption("Miembros", membersCount, selected == CircleTab.MEMBERS, Modifier.weight(1f)) { onSelect(CircleTab.MEMBERS) }
    }
}

@Composable
private fun TabOption(label: String, count: Int?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count != null) "$label · $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CirclePostRow(post: CirclePost, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NexusPostCard(
        content = post.content.ifBlank { attachmentSummary(post) },
        authorName = post.authorName,
        authorPhotoUrl = post.authorPhotoUrl,
        createdAt = post.createdAt,
        onClick = onClick,
        coverUrls = listOfNotNull(post.imageUrl),
        label = post.mood,
        modifier = modifier,
        stats = { onCover ->
            NexusPostStat(
                count = post.reactionCount,
                onCover = onCover,
                icon = Icons.Outlined.FavoriteBorder,
                contentDescription = "Reacciones",
            )
            NexusPostStat(
                count = post.ecoCount,
                onCover = onCover,
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Ecos",
            )
        },
    )
}

@Composable
fun PendingPostRow(
    post: PendingCirclePost,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NexusCard(modifier = modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (post.failed) "No se pudo publicar" else "Publicando…",
                style = MaterialTheme.typography.labelLarge,
                color = if (post.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (post.failed) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            } else {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NexusAccent)
            }
        }
        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(post.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        if (post.failed) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDiscard) { Text("Descartar") }
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
fun MemberRow(
    member: CircleMember,
    canRemove: Boolean,
    onMessage: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NexusCard(modifier = modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = member.name, photoUrl = member.photoUrl, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (member.isMe) "${member.name} (tú)" else member.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (member.isOwner) {
                    Spacer(Modifier.height(4.dp))
                    NexusPill(text = "Creador", icon = Icons.Outlined.WorkspacePremium)
                }
            }
            if (!member.isMe) {
                IconButton(onClick = onMessage) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Escribirle a ${member.name}")
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.PersonRemove,
                        contentDescription = "Quitar a ${member.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun PostRowSkeleton(modifier: Modifier = Modifier) {
    NexusCard(modifier = modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).shimmer(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.fillMaxWidth(0.4f).height(14.dp).shimmer())
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).shimmer())
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(12.dp).shimmer())
    }
}

private fun attachmentSummary(post: CirclePost): String = when {
    post.imageCount > 1 -> "${post.imageCount} fotos"
    post.imageCount == 1 -> "Foto"
    post.hasAudio -> "Nota de voz"
    else -> "Publicación"
}
