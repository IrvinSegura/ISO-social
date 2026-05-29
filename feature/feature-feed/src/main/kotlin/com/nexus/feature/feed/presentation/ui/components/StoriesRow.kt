package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.core.ui.theme.NexusAccentTeal
import com.nexus.core.ui.theme.NexusPurple
import com.nexus.feature.feed.domain.model.Story

@Composable
fun StoriesRow(
    stories: List<Story>,
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

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AddStoryItem(onClick = onAddStory)
        }
        items(groups, key = { it.first().authorId }) { group ->
            val anyUnseen = group.any { !it.hasBeenSeen }
            val startIndex = group.indexOfFirst { !it.hasBeenSeen }.coerceAtLeast(0)
            StoryGroupItem(
                group = group,
                hasUnseen = anyUnseen,
                onClick = { onStoryGroupClick(group, startIndex) },
            )
        }
    }
}

@Composable
private fun AddStoryItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "👤", fontSize = 28.sp)
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar historia",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tu historia",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StoryGroupItem(
    group: List<Story>,
    hasUnseen: Boolean,
    onClick: () -> Unit,
) {
    val representative = group.first()
    val seenColor = MaterialTheme.colorScheme.outlineVariant
    val ringBrush = if (hasUnseen) {
        Brush.sweepGradient(listOf(NexusPurple, NexusAccentTeal, NexusPurple))
    } else {
        Brush.sweepGradient(listOf(seenColor, seenColor))
    }
    val hasReel = group.any { it.type.name == "REEL" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ringBrush, CircleShape)
                .padding(3.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (representative.authorAvatarUrl != null) {
                NexusAsyncImage(
                    model = representative.authorAvatarUrl,
                    contentDescription = null,
                    shape = CircleShape,
                    fallbackInitial = representative.authorName,
                    modifier = Modifier.size(58.dp),
                )
            } else {
                Text(
                    text = representative.authorName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Indicador de reel en la esquina inferior
            if (hasReel) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(NexusPurple, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(text = "▶", fontSize = 7.sp, color = Color.White)
                }
            }

            // Contador de historias si hay más de una
            if (group.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.size.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = representative.authorName.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            color = if (hasUnseen)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
