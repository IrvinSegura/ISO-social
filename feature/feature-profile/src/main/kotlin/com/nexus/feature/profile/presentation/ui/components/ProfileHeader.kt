package com.nexus.feature.profile.presentation.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.NexusCard
import com.nexus.core.ui.components.NexusHeaderIconButton
import com.nexus.core.ui.components.NexusPageHeader
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.components.shimmer
import com.nexus.core.ui.theme.NexusAccent
import com.nexus.feature.profile.domain.model.ProfileStats
import com.nexus.feature.profile.domain.model.ProfileUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AvatarSize = 68.dp

/**
 * Top of the profile, laid out like the other tabs: the page title with the settings button,
 * then one card with who you are and your totals, as the author box of an IRSO BLOG entry.
 */
@Composable
fun ProfileHeader(
    user: ProfileUser?,
    stats: ProfileStats,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        NexusPageHeader(
            title = "Perfil",
            subtitle = "Lo que has publicado y lo que guardaste para después.",
        ) {
            NexusHeaderIconButton(icon = Icons.Outlined.Settings, contentDescription = "Ajustes", onClick = onSettingsClick)
        }

        Spacer(Modifier.height(20.dp))

        NexusCard(contentPadding = 0.dp) {
            Identity(user = user, modifier = Modifier.padding(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatsRow(stats = stats)
        }
    }
}

@Composable
private fun Identity(user: ProfileUser?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // An amber ring around the photo: the one touch of color in the card
        Box(
            modifier = Modifier
                .border(2.dp, NexusAccent, CircleShape)
                .padding(3.dp),
        ) {
            if (user == null) {
                Box(Modifier.size(AvatarSize).shimmer(CircleShape))
            } else {
                UserAvatar(name = user.name, photoUrl = user.photoUrl, size = AvatarSize)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            if (user == null) {
                Box(Modifier.width(150.dp).height(22.dp).shimmer())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(110.dp).height(14.dp).shimmer())
                return@Column
            }
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            user.email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            user.memberSince?.let { since ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NexusAccent,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "En Nexus desde ${formatMonthYear(since)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The three totals side by side, split by hairlines like a blog post's stats bar. */
@Composable
private fun StatsRow(stats: ProfileStats) {
    val suffix = if (stats.isPartial) "+" else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Stat(value = stats.posts, suffix = suffix, label = "Publicaciones", modifier = Modifier.weight(1f))
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
        Stat(value = stats.ecosReceived, suffix = suffix, label = "Ecos recibidos", modifier = Modifier.weight(1f))
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxHeight())
        Stat(value = stats.reactionsReceived, suffix = suffix, label = "Reacciones", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: Int, suffix: String, label: String, modifier: Modifier = Modifier) {
    // Counts up from zero the first time, then animates the difference on refresh
    val animated by animateIntAsState(value, animationSpec = tween(700), label = "stat")
    Column(
        modifier = modifier.padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = compactCount(animated) + suffix,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f).replace(".0M", "M")
    value >= 10_000 -> "${value / 1_000}k"
    value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000f).replace(".0k", "k")
    else -> value.toString()
}

private fun formatMonthYear(millis: Long): String =
    SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "MX")).format(Date(millis))
