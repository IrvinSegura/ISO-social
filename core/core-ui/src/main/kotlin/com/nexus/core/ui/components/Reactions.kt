package com.nexus.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nexus.core.ui.theme.Amber400
import com.nexus.core.ui.theme.Amber500
import com.nexus.core.ui.theme.Gray900

/** A reaction and the words for it, shown in the picker and read by TalkBack. */
data class NexusReaction(val emoji: String, val label: String)

/** The five reactions a post can get. The heart is the quick one (a single tap). */
val NEXUS_REACTIONS = listOf(
    NexusReaction("❤️", "Me encanta"),
    NexusReaction("🔥", "Increíble"),
    NexusReaction("💡", "Inspirador"),
    NexusReaction("🤔", "Me hace pensar"),
    NexusReaction("🌱", "Me nutre"),
)

const val QUICK_REACTION = "❤️"

fun reactionLabel(emoji: String): String = NEXUS_REACTIONS.firstOrNull { it.emoji == emoji }?.label ?: emoji

/**
 * The reaction counter of a post card: the most used emoji overlapped, then the total, which
 * rolls up or down when it changes. A tap gives (or takes back) the quick reaction; a long
 * press opens [ReactionPicker] above it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionCounter(
    reactions: Map<String, Int>,
    myReaction: String?,
    onCover: Boolean,
    pickerOpen: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDismissPicker: () -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = reactions.values.sum().coerceAtLeast(0)
    val color = when {
        myReaction != null -> if (onCover) Amber400 else Amber500
        onCover -> Color.White.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    // The emoji the user picked jumps a little, like a key being pressed
    val bump = remember { Animatable(1f) }
    LaunchedEffect(myReaction) {
        if (myReaction != null) {
            bump.snapTo(0.6f)
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = if (myReaction == null) "Reaccionar con ${reactionLabel(QUICK_REACTION)}" else "Quitar reacción",
                    onLongClickLabel = "Elegir reacción",
                    onClick = onTap,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReactionStack(
                reactions = reactions,
                myReaction = myReaction,
                onCover = onCover,
                modifier = Modifier.scale(bump.value),
            )
            Spacer(Modifier.width(5.dp))
            RollingCount(count = total, color = color, bold = myReaction != null)
        }
        ReactionPicker(
            expanded = pickerOpen,
            selected = myReaction,
            onDismiss = onDismissPicker,
            onPick = onPick,
        )
    }
}

/**
 * Up to three emoji, the most used first, overlapped like avatars. With no reactions it is an
 * empty heart outline, so the counter still says what it is.
 */
@Composable
fun ReactionStack(
    reactions: Map<String, Int>,
    myReaction: String?,
    onCover: Boolean,
    modifier: Modifier = Modifier,
) {
    val top = reactions.filterValues { it > 0 }.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
        .let { emojis -> if (myReaction != null && myReaction !in emojis.take(3)) listOf(myReaction) + emojis else emojis }
        .take(3)
    val ring = if (onCover) Gray900 else MaterialTheme.colorScheme.surface
    if (top.isEmpty()) {
        Text("🤍", fontSize = 15.sp, modifier = modifier)
        return
    }
    // Negative spacing overlaps them like stacked avatars
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        top.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ring)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(if (onCover) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 12.sp)
            }
        }
    }
}

/** A number that slides up when it grows and down when it shrinks. */
@Composable
fun RollingCount(count: Int, color: Color, bold: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                (slideOutVertically { if (up) -it else it } + fadeOut())
        },
        label = "reactionCount",
        modifier = modifier,
    ) { value ->
        Text(
            text = value.toString(),
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
        )
    }
}

/**
 * The five reactions in a floating pill above its anchor. They pop in one after another, grow
 * under the finger and show their name, and the current one sits on an amber dot.
 */
@Composable
fun ReactionPicker(
    expanded: Boolean,
    selected: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    if (!expanded) return
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, -170),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NEXUS_REACTIONS.forEachIndexed { index, reaction ->
                    PickerEmoji(
                        reaction = reaction,
                        index = index,
                        selected = reaction.emoji == selected,
                        onClick = { onPick(reaction.emoji) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerEmoji(reaction: NexusReaction, index: Int, selected: Boolean, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(35L * index)
        appear.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val grow by animateFloatAsState(if (pressed) 1.35f else 1f, spring(dampingRatio = 0.5f), label = "grow")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // The name floats above the emoji while it is held
        Text(
            text = reaction.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer { alpha = if (pressed) 1f else 0f },
        )
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = appear.value * grow
                    scaleY = appear.value * grow
                    translationY = (1f - appear.value) * 24f - (grow - 1f) * 30f
                }
                .size(44.dp)
                .clip(CircleShape)
                .selectable(
                    selected = selected,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semanticsLabel(reaction.label),
            contentAlignment = Alignment.Center,
        ) {
            Text(reaction.emoji, fontSize = 28.sp)
        }
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (selected) Amber500 else Color.Transparent),
        )
    }
}

/**
 * The post detail's reactions: every reaction as a chip with its count. The user's own is
 * amber; a tap reacts or takes it back, with a small bounce.
 */
@Composable
fun ReactionBar(
    reactions: Map<String, Int>,
    myReaction: String?,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NEXUS_REACTIONS.forEach { reaction ->
            ReactionChip(
                reaction = reaction,
                count = reactions[reaction.emoji] ?: 0,
                selected = reaction.emoji == myReaction,
                onClick = { onReact(reaction.emoji) },
            )
        }
    }
}

@Composable
private fun ReactionChip(reaction: NexusReaction, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            bounce.snapTo(0.8f)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val border = if (selected) Amber500 else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) Amber500.copy(alpha = 0.14f) else Color.Transparent
    Row(
        modifier = Modifier
            .scale(bounce.value)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semanticsLabel("${reaction.label}, $count")
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(reaction.emoji, fontSize = 17.sp)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            RollingCount(
                count = count,
                color = if (selected) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                bold = selected,
            )
        }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(Modifier.semantics { contentDescription = label })
