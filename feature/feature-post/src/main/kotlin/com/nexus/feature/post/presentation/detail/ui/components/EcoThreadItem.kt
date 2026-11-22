package com.nexus.feature.post.presentation.detail.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.core.ui.components.RollingCount
import com.nexus.core.ui.components.UserAvatar
import com.nexus.core.ui.components.relativeTimeEs
import com.nexus.core.ui.theme.Amber500
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoThread
import com.nexus.feature.post.domain.model.EcoType

/** Replies shown before "Ver N respuestas más". */
const val COLLAPSED_REPLIES = 2

/**
 * A top-level eco with its replies indented under a thread line. Long threads show the
 * first [COLLAPSED_REPLIES] replies until expanded.
 */
@Composable
fun EcoThreadItem(
    thread: EcoThread,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onReply: (Eco) -> Unit,
    onLike: (Eco) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        EcoRow(eco = thread.root, avatarSize = 38, onReply = onReply, onLike = onLike)

        if (thread.replies.isNotEmpty()) {
            val hidden = thread.replies.size - COLLAPSED_REPLIES
            val visible = if (expanded || hidden <= 0) thread.replies else thread.replies.take(COLLAPSED_REPLIES)
            // The line ties the replies to the eco they answer
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .animateContentSize()
                    .height(IntrinsicSize.Min),
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 18.dp, end = 16.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    visible.forEach { reply ->
                        EcoRow(eco = reply, avatarSize = 28, onReply = onReply, onLike = onLike)
                    }
                }
            }
            if (hidden > 0) {
                Text(
                    text = if (expanded) "Ocultar respuestas" else "Ver $hidden ${if (hidden == 1) "respuesta" else "respuestas"} más",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 36.dp, top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onToggleExpanded)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EcoRow(
    eco: Eco,
    avatarSize: Int,
    onReply: (Eco) -> Unit,
    onLike: (Eco) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        UserAvatar(name = eco.authorName, photoUrl = null, size = avatarSize.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = eco.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " · ${relativeTimeEs(eco.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(3.dp))
            EcoContent(eco)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = "Responder",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = "Responder a ${eco.authorName}") { onReply(eco) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                )
            }
        }
        EcoHeart(eco = eco, onLike = { onLike(eco) })
    }
}

@Composable
private fun EcoContent(eco: Eco) {
    val mention = eco.replyToName
    when (eco.type) {
        EcoType.TEXT -> Text(
            text = buildAnnotatedString {
                if (mention != null) {
                    withStyle(SpanStyle(color = Amber500, fontWeight = FontWeight.SemiBold)) { append("@$mention ") }
                }
                append(eco.message)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        else -> Column {
            if (mention != null) {
                Text("@$mention", color = Amber500, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
            when (eco.type) {
                EcoType.EMOJI -> Text(eco.emoji ?: "😊", fontSize = 40.sp)
                EcoType.GIF, EcoType.IMAGE -> eco.mediaUrl?.let { url ->
                    NexusAsyncImage(
                        model = url,
                        contentDescription = if (eco.type == EcoType.GIF) "GIF" else "Imagen",
                        contentScale = if (eco.type == EcoType.GIF) ContentScale.Fit else ContentScale.Crop,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .widthIn(max = 220.dp)
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 180.dp),
                    )
                }
                EcoType.AUDIO -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nota de voz", style = MaterialTheme.typography.labelLarge)
                }
                EcoType.TEXT -> Unit
            }
        }
    }
}

/** A heart that pops when given, with its count rolling under it. */
@Composable
private fun EcoHeart(eco: Eco, onLike: () -> Unit) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(eco.likedByMe) {
        if (eco.likedByMe) {
            pop.snapTo(0.5f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val tint = if (eco.likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = if (eco.likedByMe) "Quitar corazón" else "Dar corazón",
                onClick = onLike,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = if (eco.likedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .scale(pop.value),
        )
        if (eco.likeCount > 0) {
            RollingCount(count = eco.likeCount, color = tint, bold = eco.likedByMe)
        }
    }
}
