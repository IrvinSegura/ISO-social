package com.nexus.feature.post.presentation.create.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.core.ui.components.CircleBadge
import com.nexus.core.ui.theme.NexusAccent
import com.nexus.core.ui.theme.circleColor

/** "Público ▾" or the chosen circle, under the author's name. */
@Composable
fun AudienceChip(
    circle: CircleSummary?,
    isLoadingCircle: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = circle?.let { circleColor(it.colorIndex) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (circle != null) color.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.semantics { role = Role.DropdownList },
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                circle != null -> Text(circle.emoji)
                isLoadingCircle -> Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(14.dp), tint = color)
                else -> Icon(Icons.Outlined.Public, null, modifier = Modifier.size(14.dp), tint = color)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = when {
                    circle != null -> circle.name
                    isLoadingCircle -> "Cargando círculo…"
                    else -> "Público"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (circle != null) color else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Cambiar audiencia", modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiencePickerSheet(
    circles: List<CircleSummary>,
    selectedCircleId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("¿Quién puede verlo?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "public") {
                    AudienceOption(
                        title = "Público",
                        subtitle = "Todos en Nexus, en el feed de Inicio",
                        selected = selectedCircleId == null,
                        onClick = { onSelect(null) },
                        leading = {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.Public, null) }
                        },
                    )
                }
                items(circles, key = { it.id }) { circle ->
                    val count = circle.memberIds.size
                    AudienceOption(
                        title = circle.name,
                        subtitle = "Solo sus ${if (count == 1) "1 miembro" else "$count miembros"}",
                        selected = circle.id == selectedCircleId,
                        onClick = { onSelect(circle.id) },
                        leading = { CircleBadge(emoji = circle.emoji, colorIndex = circle.colorIndex, size = 40.dp) },
                    )
                }
                if (circles.isEmpty()) {
                    item(key = "none") {
                        Text(
                            text = "Crea o únete a un círculo en la pestaña Círculos para publicar solo para un grupo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AudienceOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) NexusAccent else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NexusAccent)
        }
    }
}
