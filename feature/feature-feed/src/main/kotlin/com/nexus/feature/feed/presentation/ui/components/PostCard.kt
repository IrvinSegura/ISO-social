package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.nexus.core.ui.theme.NexusAccentTeal
import com.nexus.core.ui.theme.NexusPurple
import com.nexus.feature.feed.domain.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PRESET_REACTIONS = listOf("❤️", "🤔", "💡", "🌱", "🔥")

@Composable
fun PostCard(
    post: Post,
    userReaction: String? = null,
    ecoCountDelta: Int = 0,
    onEcoClick: () -> Unit,
    onClick: () -> Unit,
    onReactionClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accentColor = avatarColor(post.authorName)
    val isDark = isSystemInDarkTheme()
    val hasImage = post.imageUrls.isNotEmpty()

    // En claro: sombra coloreada. En oscuro: sin sombra, brilla por el tinte.
    val cardElevation = if (isDark) 0.dp else 2.dp

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // Tinte de color del autor — más pronunciado en claro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = if (isDark) 0.14f else 0.09f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column {
                // ── HEADER ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StoryAvatar(
                        photoUrl = post.authorPhotoUrl,
                        name = post.authorName,
                        accentColor = accentColor,
                        modifier = Modifier.clickable { onAuthorClick(post.authorId) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = post.authorName,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.4).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (post.mood != null) {
                                MoodBadge(mood = post.mood, accentColor = accentColor, isDark = isDark)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        PostMeta(
                            time = formatRelativeTime(post.createdAt),
                            location = post.locationName,
                            circle = post.circleName,
                            accentColor = accentColor,
                        )
                    }
                }

                // ── CONTENIDO ─────────────────────────────────────────────
                if (post.content.isNotBlank()) {
                    Text(
                        text = post.content,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.88f else 0.82f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // ── MEDIA ─────────────────────────────────────────────────
                if (hasImage) {
                    Spacer(Modifier.height(10.dp))
                    PostImagesPager(imageUrls = post.imageUrls, timestamp = post.createdAt)
                }
                if (post.audioUrl != null) {
                    Spacer(Modifier.height(10.dp))
                    AudioRow(modifier = Modifier.padding(horizontal = 14.dp))
                }
                if (post.fileUrl != null && post.fileName != null) {
                    Spacer(Modifier.height(10.dp))
                    FileRow(fileName = post.fileName, modifier = Modifier.padding(horizontal = 14.dp))
                }

                // ── REACCIONES ────────────────────────────────────────────
                ReactionsBar(
                    reactions = post.reactions,
                    ecoCount = post.ecoCount + ecoCountDelta,
                    userReaction = userReaction,
                    accentColor = accentColor,
                    isDark = isDark,
                    hasMedia = hasImage || post.audioUrl != null || post.fileUrl != null,
                    onReactionClick = onReactionClick,
                    onEcoClick = onEcoClick,
                )
            }
        }
    }
}

// ── Mood badge ───────────────────────────────────────────────────────────────

@Composable
private fun MoodBadge(mood: String, accentColor: Color, isDark: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = mood,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) accentColor.copy(alpha = 0.95f) else accentColor,
        )
    }
}

// ── Post meta ────────────────────────────────────────────────────────────────

@Composable
private fun PostMeta(time: String, location: String?, circle: String?, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = time,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        if (location != null) {
            MetaDot()
            Icon(
                Icons.Outlined.LocationOn, null,
                modifier = Modifier.size(11.dp),
                tint = NexusAccentTeal,
            )
            Text(
                text = location,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (circle != null) {
            MetaDot()
            Text(
                text = "#$circle",
                fontSize = 11.5.sp,
                color = NexusAccentTeal,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MetaDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
    )
}

// ── Story Avatar ─────────────────────────────────────────────────────────────

@Composable
private fun StoryAvatar(
    photoUrl: String?,
    name: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(52.dp), contentAlignment = Alignment.Center) {
        // Ring gradiente exterior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accentColor, NexusAccentTeal))),
        )
        // Gap blanco/superficie entre ring y foto
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
        // Foto o inicial
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl).crossfade(true).build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(42.dp).clip(CircleShape),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accentColor, NexusAccentTeal))),
            ) {
                Text(
                    text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Reactions bar ────────────────────────────────────────────────────────────

@Composable
private fun ReactionsBar(
    reactions: Map<String, Int>,
    ecoCount: Int,
    userReaction: String?,
    accentColor: Color,
    isDark: Boolean,
    hasMedia: Boolean,
    onReactionClick: (String) -> Unit,
    onEcoClick: () -> Unit,
) {
    Column {
        Spacer(Modifier.height(if (hasMedia) 0.dp else 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(0.8.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                            NexusAccentTeal.copy(alpha = if (isDark) 0.25f else 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PRESET_REACTIONS.forEach { emoji ->
                    ReactionPill(
                        emoji = emoji,
                        count = reactions[emoji] ?: 0,
                        isSelected = emoji == userReaction,
                        accentColor = accentColor,
                        isDark = isDark,
                        onClick = { onReactionClick(emoji) },
                    )
                }
            }
            EcoPill(
                ecoCount = ecoCount,
                accentColor = accentColor,
                isDark = isDark,
                onClick = onEcoClick,
            )
        }
    }
}

@Composable
private fun ReactionPill(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pill_scale",
    )
    val showCount = count > 0 || isSelected

    // Fondo: en claro más visible, en oscuro más sutil
    val inactiveBg = if (isDark)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceContainerHighest

    val countedBg = accentColor.copy(alpha = if (isDark) 0.15f else 0.1f)

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> Brush.linearGradient(listOf(accentColor, NexusAccentTeal))
                    count > 0 -> Brush.linearGradient(listOf(countedBg, countedBg))
                    else -> Brush.linearGradient(listOf(inactiveBg, inactiveBg))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (showCount) 9.dp else 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = emoji, fontSize = 17.sp)
            AnimatedVisibility(
                visible = showCount,
                enter = expandHorizontally() + fadeIn(),
            ) {
                Text(
                    text = if (count > 0) count.toString() else "1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else accentColor,
                )
            }
        }
    }
}

@Composable
private fun EcoPill(
    ecoCount: Int,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val hasEcos = ecoCount > 0
    val bgColor = when {
        hasEcos -> accentColor.copy(alpha = if (isDark) 0.18f else 0.11f)
        else -> if (isDark)
            MaterialTheme.colorScheme.surfaceContainerHigh
        else
            MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                Icons.Filled.ChatBubble, null,
                modifier = Modifier.size(13.dp),
                tint = if (hasEcos) accentColor
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.5f else 0.45f),
            )
            Text(
                text = if (hasEcos) "$ecoCount" else "Eco",
                fontSize = 12.5.sp,
                fontWeight = if (hasEcos) FontWeight.Bold else FontWeight.Medium,
                color = if (hasEcos) accentColor
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.5f else 0.45f),
            )
        }
    }
}

// ── Images Pager ─────────────────────────────────────────────────────────────

@Composable
private fun PostImagesPager(imageUrls: List<String>, timestamp: Long) {
    val pagerState = rememberPagerState { imageUrls.size }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrls[page]).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
                ),
        )

        // Fecha con pill backdrop
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 11.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = formatDate(timestamp),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f),
            )
        }

        // Dots — pill activo gradiente, inactivos pequeños
        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(imageUrls.size) { i ->
                    val isActive = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 20.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive)
                                    Brush.linearGradient(listOf(NexusPurple, NexusAccentTeal))
                                else
                                    Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.4f)),
                                    ),
                            ),
                    )
                }
            }
        }
    }
}

// ── Audio ─────────────────────────────────────────────────────────────────────

@Composable
private fun AudioRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(NexusPurple.copy(alpha = 0.1f), NexusAccentTeal.copy(alpha = 0.08f)),
                ),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexusPurple, NexusAccentTeal))),
            ) {
                Icon(Icons.Outlined.PlayCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Nota de voz", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Toca para escuchar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.AudioFile, null, tint = NexusPurple, modifier = Modifier.size(18.dp))
        }
    }
}

// ── File ──────────────────────────────────────────────────────────────────────

@Composable
private fun FileRow(fileName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NexusPurple.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Outlined.Description, null, tint = NexusPurple, modifier = Modifier.size(20.dp))
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun avatarColor(name: String): Color {
    val palette = listOf(
        Color(0xFF6C63FF), Color(0xFF5C6BC0), Color(0xFFE91E8C),
        Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF4CAF50),
        Color(0xFFFF9800), Color(0xFF9C27B0),
    )
    return palette[name.hashCode().and(0x7FFFFFFF) % palette.size]
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

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d.MM.yyyy", Locale("es")).format(Date(timestamp))
