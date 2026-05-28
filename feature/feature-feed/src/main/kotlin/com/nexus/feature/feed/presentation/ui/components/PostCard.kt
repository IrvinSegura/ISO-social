package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nexus.feature.feed.domain.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PRESET_REACTIONS = listOf("❤️", "🤔", "💡", "🌱", "🔥")

@Composable
fun PostCard(
    post: Post,
    userReaction: String? = null,
    onEcoClick: () -> Unit,
    onClick: () -> Unit,
    onReactionClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PostAvatar(
                    photoUrl = post.authorPhotoUrl,
                    name = post.authorName,
                    modifier = Modifier.clickable { onAuthorClick(post.authorId) },
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatRelativeTime(post.createdAt),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (post.locationName != null) {
                            Text(
                                text = "·",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = post.locationName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Mood pill
                if (post.mood != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = post.mood,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // ── Media ────────────────────────────────────────────────────
            if (post.imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                PostImagesPager(imageUrls = post.imageUrls)
            }

            if (post.audioUrl != null) {
                Spacer(Modifier.height(10.dp))
                AudioRow(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (post.fileUrl != null && post.fileName != null) {
                Spacer(Modifier.height(10.dp))
                FileRow(
                    fileName = post.fileName,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // ── Divider ──────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            // ── Actions bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Emoji reactions
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PRESET_REACTIONS.forEach { emoji ->
                        val count = post.reactions[emoji] ?: 0
                        ReactionChip(
                            emoji = emoji,
                            count = count,
                            isSelectedByUser = emoji == userReaction,
                            onClick = { onReactionClick(emoji) },
                        )
                    }
                }

                // Eco button
                EcoActionButton(
                    ecoCount = post.ecoCount,
                    onClick = onEcoClick,
                )
            }
        }
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    count: Int,
    isSelectedByUser: Boolean,
    onClick: () -> Unit,
) {
    val isActive = count > 0
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelectedByUser -> MaterialTheme.colorScheme.primary
            isActive -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reactionColor",
    )
    val textColor = when {
        isSelectedByUser -> MaterialTheme.colorScheme.onPrimary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (isSelectedByUser) 1.08f else if (isActive) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "reactionScale",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.scale(scale),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isActive || isSelectedByUser) 7.dp else 6.dp,
                vertical = 5.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = emoji, fontSize = 15.sp)
            AnimatedVisibility(
                visible = isActive || isSelectedByUser,
                enter = expandHorizontally() + fadeIn(),
            ) {
                Text(
                    text = if (count > 0) count.toString() else "1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun EcoActionButton(ecoCount: Int, onClick: () -> Unit) {
    val hasEcos = ecoCount > 0
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (hasEcos) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = "Eco",
                modifier = Modifier.size(14.dp),
                tint = if (hasEcos) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (hasEcos) ecoCount.toString() else "Eco",
                fontSize = 12.sp,
                fontWeight = if (hasEcos) FontWeight.SemiBold else FontWeight.Normal,
                color = if (hasEcos) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PostImagesPager(imageUrls: List<String>) {
    val pagerState = rememberPagerState { imageUrls.size }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrls[page])
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gradient overlay at bottom
        if (imageUrls.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(imageUrls.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.5f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioRow(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Reproducir audio",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Nota de voz",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Toca para escuchar",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.AudioFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FileRow(fileName: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = fileName,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PostAvatar(photoUrl: String?, name: String, modifier: Modifier = Modifier) {
    if (photoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl).crossfade(true).build(),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(42.dp)
                .clip(CircleShape),
        )
    } else {
        val color = avatarColor(name)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color),
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 17.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
        Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043),
        Color(0xFF66BB6A), Color(0xFFEC407A),
    )
    return colors[name.hashCode().and(0x7FFFFFFF) % colors.size]
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "ahora"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("d MMM", Locale("es")).format(Date(timestamp))
    }
}
