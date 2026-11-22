package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.NexusPostCard
import com.nexus.core.ui.components.NexusPostStat
import com.nexus.core.ui.components.QUICK_REACTION
import com.nexus.core.ui.components.ReactionCounter
import com.nexus.core.ui.share.ShareSheet
import com.nexus.core.ui.share.SharedPost
import com.nexus.feature.feed.domain.model.Post


@Composable
fun PostCard(
    post: Post,
    onEcoClick: () -> Unit,
    onClick: () -> Unit,
    onReactionClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    isSaved: Boolean = false,
    onSaveClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var showShare by remember { mutableStateOf(false) }
    // A circle's posts stay inside it, so only public ones can be shared
    val canShare = post.circleId == null
    val react: (String) -> Unit = { emoji ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onReactionClick(emoji)
    }

    NexusPostCard(
        content = post.content,
        authorName = post.authorName,
        authorPhotoUrl = post.authorPhotoUrl,
        createdAt = post.createdAt,
        onClick = onClick,
        // Photos first; a post with only a GIF uses it as the cover
        coverUrls = post.imageUrls.ifEmpty { listOfNotNull(post.gifUrl) },
        label = post.mood ?: post.locationName?.let { "📍 $it" },
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        topEnd = { onCover ->
            PostOptions(
                isSaved = isSaved,
                canReport = !post.isOwnPost,
                onCover = onCover,
                onSaveClick = onSaveClick,
                onReportClick = onReportClick,
                onShareClick = if (canShare) ({ showShare = true }) else null,
            )
        },
        stats = { onCover ->
            ReactionStat(
                reactions = post.reactions,
                myReaction = post.myReaction,
                onCover = onCover,
                onReact = react,
            )
            NexusPostStat(
                count = post.ecoCount,
                onCover = onCover,
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Ecos",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEcoClick()
                },
            )
        },
        attachments = if (post.audioUrl != null || post.fileName != null) {
            { onCover -> Attachments(post, onCover) }
        } else {
            null
        },
    )

    if (showShare && canShare) {
        ShareSheet(
            post = SharedPost(
                id = post.id,
                content = post.content,
                authorName = post.authorName,
                mood = post.mood,
                imageUrl = post.imageUrls.firstOrNull() ?: post.gifUrl,
            ),
            onDismiss = { showShare = false },
        )
    }
}

// ── Reactions ────────────────────────────────────────────────────────────────

/**
 * One counter for every reaction: the most used emoji and the total. A tap gives (or takes
 * back) a heart; a long press opens the five reactions above it.
 */
@Composable
private fun ReactionStat(
    reactions: Map<String, Int>,
    myReaction: String?,
    onCover: Boolean,
    onReact: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    ReactionCounter(
        reactions = reactions,
        myReaction = myReaction,
        onCover = onCover,
        pickerOpen = pickerOpen,
        // The same emoji again removes the reaction
        onTap = { onReact(myReaction ?: QUICK_REACTION) },
        onLongPress = { pickerOpen = true },
        onDismissPicker = { pickerOpen = false },
        onPick = { emoji ->
            pickerOpen = false
            onReact(emoji)
        },
    )
}

// ── Options ──────────────────────────────────────────────────────────────────

// A saved post shows its bookmark next to the menu, so the state is visible at a glance
@Composable
private fun PostOptions(
    isSaved: Boolean,
    canReport: Boolean,
    onCover: Boolean,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val tint = if (onCover) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // On a photo the icons sit on a dark chip so they stay visible on any image
        modifier = if (onCover) {
            Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        } else {
            Modifier
        },
    ) {
        AnimatedVisibility(visible = isSaved, enter = fadeIn() + expandHorizontally()) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "Guardada",
                tint = if (onCover) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
            )
        }
        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = tint)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (isSaved) "Quitar de guardados" else "Guardar") },
                    leadingIcon = {
                        Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onSaveClick()
                    },
                )
                if (onShareClick != null) {
                    DropdownMenuItem(
                        text = { Text("Compartir") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onShareClick()
                        },
                    )
                }
                if (canReport) {
                    DropdownMenuItem(
                        text = { Text("Reportar") },
                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onReportClick()
                        },
                    )
                }
            }
        }
    }
}

// ── Attachments ──────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.Attachments(post: Post, onCover: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (post.audioUrl != null) {
            AttachmentChip(icon = Icons.Outlined.GraphicEq, label = "Nota de voz", onCover = onCover)
        }
        if (post.audioUrl != null && post.fileName != null) Spacer(Modifier.width(8.dp))
        post.fileName?.let { AttachmentChip(icon = Icons.Outlined.Description, label = it, onCover = onCover) }
    }
}

@Composable
private fun AttachmentChip(icon: ImageVector, label: String, onCover: Boolean) {
    val color = if (onCover) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (onCover) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}
