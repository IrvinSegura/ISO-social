package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.theme.Amber300
import com.nexus.core.ui.theme.Amber400
import com.nexus.core.ui.theme.Amber600
import com.nexus.core.ui.theme.Gray900
import com.nexus.feature.feed.domain.model.Story
import com.nexus.feature.feed.domain.repository.StoryMe

private val AvatarSize = 60.dp
private val TileWidth = 72.dp

/**
 * The "Historias" section of Inicio: your own tile first (your photo with an amber +), then
 * one tile per person, unseen ones ringed in amber and seen ones in a hairline.
 */
@Composable
fun StoriesRow(
    stories: List<Story>,
    me: StoryMe?,
    onAddStory: () -> Unit,
    // Devuelve la lista de historias del grupo y el índice de inicio (primera no vista o 0)
    onStoryGroupClick: (stories: List<Story>, startIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Agrupa por autor manteniendo el orden de la historia más reciente por grupo
    val groups = stories
        .groupBy { it.authorId }
        .values
        .sortedByDescending { group -> group.maxOf { it.createdAt } }
    // Your own stories live in your tile, not in the list of others
    val mine = groups.firstOrNull { it.first().authorId == me?.uid }.orEmpty()
    val others = groups.filter { it.first().authorId != me?.uid }
    // Unseen first: what is new is what people come to the row for
    val ordered = others.sortedByDescending { group -> group.any { !it.hasBeenSeen } }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Historias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "  ·  duran 24 h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "me") {
                MyStoryTile(
                    me = me,
                    mine = mine,
                    onAdd = onAddStory,
                    onOpen = { onStoryGroupClick(mine, 0) },
                )
            }
            items(ordered, key = { it.first().authorId }) { group ->
                val startIndex = group.indexOfFirst { !it.hasBeenSeen }.coerceAtLeast(0)
                StoryGroupTile(
                    group = group,
                    hasUnseen = group.any { !it.hasBeenSeen },
                    onClick = { onStoryGroupClick(group, startIndex) },
                )
            }
        }
    }
}

/** Tapping opens your stories if you have any; the + always adds a new one. */
@Composable
private fun MyStoryTile(
    me: StoryMe?,
    mine: List<Story>,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
) {
    val hasStories = mine.isNotEmpty()
    val name = me?.name ?: mine.firstOrNull()?.authorName ?: "Tú"
    val photo = me?.photoUrl ?: mine.firstOrNull()?.authorAvatarUrl
    Tile(
        label = "Tu historia",
        emphasized = true,
        onClick = if (hasStories) onOpen else onAdd,
        onClickLabel = if (hasStories) "Ver tu historia" else "Agregar historia",
    ) {
        Box {
            Ring(active = false, visible = hasStories) {
                UserAvatar(name = name, photoUrl = photo, size = AvatarSize)
            }
            // Amber + with a ring of the background, so it reads on any photo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(24.dp)
                    .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(Amber400)
                    .clickable(role = Role.Button, onClickLabel = "Agregar historia", onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar historia", tint = Gray900, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun StoryGroupTile(group: List<Story>, hasUnseen: Boolean, onClick: () -> Unit) {
    val representative = group.first()
    val hasReel = group.any { it.type.name == "REEL" }
    Tile(
        label = representative.authorName.split(" ").first(),
        emphasized = hasUnseen,
        onClick = onClick,
        onClickLabel = "Ver historias de ${representative.authorName}",
    ) {
        Box {
            Ring(active = hasUnseen, visible = true) {
                UserAvatar(
                    name = representative.authorName,
                    photoUrl = representative.authorAvatarUrl,
                    size = AvatarSize,
                )
            }
            when {
                // More than one story: how many, in an amber chip on the corner
                group.size > 1 -> CornerChip(Modifier.align(Alignment.TopEnd)) {
                    Text(
                        text = group.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gray900,
                    )
                }
                hasReel -> CornerChip(Modifier.align(Alignment.BottomEnd)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Video", tint = Gray900, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

/** Avatar plus a ring: amber gradient while unseen, a hairline once seen, none for an empty tile. */
@Composable
private fun Ring(active: Boolean, visible: Boolean, content: @Composable () -> Unit) {
    val width: Dp = if (active) 2.5.dp else 1.5.dp
    val brush = if (active) {
        Brush.sweepGradient(listOf(Amber300, Amber600, Amber400, Amber300))
    } else {
        Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline),
        )
    }
    Box(
        modifier = Modifier
            .then(if (visible) Modifier.border(width, brush, CircleShape) else Modifier)
            .padding(3.5.dp),
    ) {
        content()
    }
}

@Composable
private fun CornerChip(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(22.dp)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(Amber400),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun Tile(
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit,
    onClickLabel: String,
    avatar: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(TileWidth + 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        avatar()
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(TileWidth),
        )
    }
}
