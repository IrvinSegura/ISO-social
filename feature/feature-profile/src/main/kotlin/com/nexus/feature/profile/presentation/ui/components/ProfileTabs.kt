package com.nexus.feature.profile.presentation.ui.components

import com.nexus.core.ui.components.NexusEmptyState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.feature.profile.presentation.ProfileTab

/** Pill switch between the user's posts and the saved ones, with their counts. */
@Composable
fun ProfileTabs(
    selected: ProfileTab,
    postsCount: Int?,
    savedCount: Int?,
    onSelect: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabPill(
            icon = Icons.AutoMirrored.Outlined.Article,
            label = "Publicaciones",
            count = postsCount,
            selected = selected == ProfileTab.POSTS,
            onClick = { onSelect(ProfileTab.POSTS) },
            modifier = Modifier.weight(1f),
        )
        TabPill(
            icon = Icons.Outlined.BookmarkBorder,
            label = "Guardados",
            count = savedCount,
            selected = selected == ProfileTab.SAVED,
            onClick = { onSelect(ProfileTab.SAVED) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabPill(
    icon: ImageVector,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "tabBackground",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, fontWeight = FontWeight.SemiBold)
        if (count != null && count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = compactCount(count),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                modifier = Modifier
                    .background(content.copy(alpha = 0.16f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

/** Centered message for an empty or failed tab, with an optional action (the shared blog style). */
@Composable
fun ProfileEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") isError: Boolean = false,
) {
    NexusEmptyState(
        icon = icon,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}
