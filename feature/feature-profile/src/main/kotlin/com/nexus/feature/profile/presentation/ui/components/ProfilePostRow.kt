package com.nexus.feature.profile.presentation.ui.components

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.core.ui.components.shimmer
import com.nexus.core.ui.components.splitPostText
import com.nexus.core.ui.theme.Amber400
import com.nexus.core.ui.theme.Gray900
import androidx.compose.ui.unit.sp
import com.nexus.feature.profile.domain.model.ProfilePost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RowShape = RoundedCornerShape(16.dp)

/**
 * One post in a profile list. [showAuthor] for saved posts, which may be anyone's;
 * [onUnsave] adds the filled bookmark that takes it out of the saved list.
 */
@Composable
fun ProfilePostRow(
    post: ProfilePost,
    showAuthor: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUnsave: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RowShape)
            .clickable(onClick = onClick, onClickLabel = "Abrir publicación")
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (showAuthor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexusAsyncImage(
                        model = post.authorPhotoUrl,
                        contentDescription = null,
                        shape = CircleShape,
                        fallbackInitial = post.authorName.take(1).uppercase(),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = " · ${relativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = relativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    post.mood?.let { mood ->
                        Spacer(Modifier.width(8.dp))
                        // The same amber pill the feed cards use for the mood
                        Text(
                            text = mood,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(Amber400, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Headline and summary, split as the feed cards split them
            val text = splitPostText(post.content)
            if (text.title == null && text.body == null) {
                Text(
                    text = attachmentSummary(post),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            text.title?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            text.body?.let {
                if (text.title != null) Spacer(Modifier.height(3.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (text.title == null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (text.title == null) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Metric(Icons.Outlined.Forum, post.ecoCount.toString(), "ecos")
                Metric(Icons.Outlined.FavoriteBorder, post.reactionCount.toString(), "reacciones")
                if (post.hasAudio) Metric(Icons.Outlined.Mic, null, "Incluye audio")
                if (post.fileName != null) Metric(Icons.Outlined.AttachFile, null, "Incluye archivo")
            }
        }

        if (post.imageUrl != null) {
            Spacer(Modifier.width(12.dp))
            Box {
                NexusAsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(80.dp),
                )
                if (post.imageCount > 1) {
                    Text(
                        text = "+${post.imageCount - 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }

        if (onUnsave != null) {
            IconButton(onClick = onUnsave, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Quitar de guardados",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, value: String?, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = if (value == null) description else null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (value != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Placeholder rows while a tab loads for the first time. */
@Composable
fun ProfilePostRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(70.dp).height(12.dp).shimmer())
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).shimmer())
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).shimmer())
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(90.dp).height(12.dp).shimmer())
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(76.dp).shimmer(RoundedCornerShape(16.dp)))
    }
}

private fun attachmentSummary(post: ProfilePost): String = when {
    post.imageCount > 0 -> if (post.imageCount == 1) "Foto" else "${post.imageCount} fotos"
    post.hasAudio -> "Nota de voz"
    post.fileName != null -> post.fileName
    else -> "Publicación"
}

internal fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - millis).coerceAtLeast(0)
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> "hace ${minutes / 60} h"
        minutes < 7 * 24 * 60 -> "hace ${minutes / (24 * 60)} d"
        else -> SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(millis))
    }
}
