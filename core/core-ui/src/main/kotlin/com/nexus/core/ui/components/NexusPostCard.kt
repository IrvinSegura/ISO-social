package com.nexus.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.theme.Amber400
import com.nexus.core.ui.theme.Gray900
import com.nexus.core.ui.theme.isNexusDarkTheme

/** A post's text split as a blog card shows it: a bold headline and the rest below. */
data class PostText(val title: String?, val body: String?)

private const val MAX_TITLE = 90
private val FIRST_SENTENCE = Regex("^(.{8,$MAX_TITLE}?[.!?…])\\s+(.+)$", RegexOption.DOT_MATCHES_ALL)

/**
 * The first line becomes the title when there is more below it; otherwise a short text is
 * all title, and a long one gives its first sentence. A long text with no short first
 * sentence has no title, so it is not shown as a wall of bold text.
 */
fun splitPostText(text: String): PostText {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return PostText(null, null)
    val firstLine = trimmed.substringBefore('\n').trim()
    val rest = trimmed.substringAfter('\n', "").trim()
    return when {
        rest.isNotEmpty() && firstLine.length <= MAX_TITLE -> PostText(firstLine, rest)
        trimmed.length <= MAX_TITLE -> PostText(trimmed, null)
        else -> FIRST_SENTENCE.find(trimmed)
            ?.let { PostText(it.groupValues[1], it.groupValues[2].trim()) }
            ?: PostText(null, trimmed)
    }
}

/**
 * Post card in the style of IRSO BLOG's featured posts. With a cover (photos or a GIF) the
 * image fills the card and the text sits on a dark gradient at the bottom; without one it
 * is a plain bordered card with the same layout.
 *
 * @param label amber pill above the title (the mood, for instance)
 * @param topEnd actions in the top corner; told whether they sit on the cover
 * @param stats counters after the author and time; told whether they sit on the cover
 * @param attachments extra rows under the text (audio, files)
 */
@Composable
fun NexusPostCard(
    content: String,
    authorName: String,
    authorPhotoUrl: String?,
    createdAt: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverUrls: List<String> = emptyList(),
    label: String? = null,
    topEnd: @Composable (onCover: Boolean) -> Unit = {},
    stats: @Composable RowScope.(onCover: Boolean) -> Unit = {},
    attachments: (@Composable ColumnScope.(onCover: Boolean) -> Unit)? = null,
) {
    val text = splitPostText(content)
    val onCover = coverUrls.isNotEmpty()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = if (onCover) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (onCover && !isNexusDarkTheme()) 3.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = if (onCover) Gray900 else MaterialTheme.colorScheme.surface),
    ) {
        if (onCover) {
            Box(Modifier.fillMaxWidth().heightIn(min = 320.dp)) {
                Cover(urls = coverUrls, modifier = Modifier.matchParentSize())
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.10f),
                                0.35f to Color.Black.copy(alpha = 0.05f),
                                0.62f to Color.Black.copy(alpha = 0.55f),
                                1f to Color.Black.copy(alpha = 0.90f),
                            ),
                        ),
                )
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { topEnd(true) }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 160.dp, bottom = 18.dp),
                ) {
                    CardText(label = label, text = text, onCover = true)
                    attachments?.let {
                        Spacer(Modifier.height(10.dp))
                        it(true)
                    }
                    Spacer(Modifier.height(14.dp))
                    MetaRow(authorName, authorPhotoUrl, createdAt, onCover = true, stats = stats)
                }
            }
        } else {
            TextOnlyBody(
                text = text,
                authorName = authorName,
                authorPhotoUrl = authorPhotoUrl,
                createdAt = createdAt,
                label = label,
                topEnd = topEnd,
                stats = stats,
                attachments = attachments,
            )
        }
    }
}

/**
 * A post without images, laid out like a blog entry: who and when on top with the menu,
 * then the headline and a summary, and a footer with the counters and the mood.
 */
@Composable
private fun TextOnlyBody(
    text: PostText,
    authorName: String,
    authorPhotoUrl: String?,
    createdAt: Long,
    label: String?,
    topEnd: @Composable (onCover: Boolean) -> Unit,
    stats: @Composable RowScope.(onCover: Boolean) -> Unit,
    attachments: (@Composable ColumnScope.(onCover: Boolean) -> Unit)?,
) {
    val soft = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 4.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(name = authorName, photoUrl = authorPhotoUrl, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = authorName,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = relativeTimeEs(createdAt), fontSize = 13.sp, lineHeight = 18.sp, color = soft, maxLines = 1)
            }
            topEnd(false)
        }

        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
            CardText(label = null, text = text, onCover = false)
            attachments?.let {
                Spacer(Modifier.height(12.dp))
                it(false)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            // The stats bring their own touch padding; pull them back to the text's edge
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stats(false)
            Spacer(Modifier.weight(1f))
            if (label != null) MoodPill(label)
        }
    }
}

@Composable
private fun MoodPill(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Gray900,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .background(Amber400)
            .padding(horizontal = 11.dp, vertical = 4.dp),
    )
}

@Composable
private fun Cover(urls: List<String>, modifier: Modifier) {
    if (urls.size == 1) {
        NexusAsyncImage(model = urls.first(), contentDescription = null, modifier = modifier)
        return
    }
    val pager = rememberPagerState { urls.size }
    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            NexusAsyncImage(model = urls[page], contentDescription = "Imagen ${page + 1}", modifier = Modifier.fillMaxSize())
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(urls.size) { i ->
                Box(
                    Modifier
                        .size(width = if (i == pager.currentPage) 18.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.5f)),
                )
            }
        }
    }
}

@Composable
private fun CardText(label: String?, text: PostText, onCover: Boolean) {
    val strong = if (onCover) Color.White else MaterialTheme.colorScheme.onSurface
    val soft = if (onCover) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    if (label != null) {
        MoodPill(label)
        Spacer(Modifier.height(12.dp))
    }
    text.title?.let {
        Text(
            text = it,
            fontSize = if (onCover) 22.sp else 20.sp,
            lineHeight = if (onCover) 28.sp else 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = strong,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
    text.body?.let {
        if (text.title != null) Spacer(Modifier.height(8.dp))
        Text(
            text = it,
            fontSize = if (text.title == null) 17.sp else 15.sp,
            lineHeight = if (text.title == null) 25.sp else 22.sp,
            fontWeight = if (text.title == null) FontWeight.Medium else FontWeight.Normal,
            color = if (text.title == null) strong else soft,
            // Without a photo the summary is all the card has, so it gets a third line
            maxLines = if (text.title == null) 5 else if (onCover) 2 else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaRow(
    authorName: String,
    authorPhotoUrl: String?,
    createdAt: Long,
    onCover: Boolean,
    stats: @Composable RowScope.(Boolean) -> Unit,
) {
    val strong = if (onCover) Color.White else MaterialTheme.colorScheme.onSurface
    val soft = if (onCover) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(
            name = authorName,
            photoUrl = authorPhotoUrl,
            size = 30.dp,
            modifier = if (onCover) Modifier.border(2.dp, Color.White, CircleShape) else Modifier,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = authorName,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = strong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        MetaDot(soft)
        Text(text = relativeTimeEs(createdAt), fontSize = 13.sp, color = soft, maxLines = 1)
        stats(onCover)
    }
}

@Composable
private fun MetaDot(color: Color) {
    Text(" · ", fontSize = 13.sp, color = color)
}

/**
 * A counter for [NexusPostCard]'s stats: an icon (or an emoji) and a number, optionally
 * tappable and long-pressable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NexusPostStat(
    count: Int,
    onCover: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val color = when {
        highlighted -> if (onCover) Amber400 else MaterialTheme.colorScheme.error
        onCover -> Color.White.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val clickable = if (onClick != null || onLongClick != null) {
        Modifier
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .padding(start = 6.dp)
            .then(clickable)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            emoji != null -> Text(emoji, fontSize = 15.sp)
            icon != null -> Icon(icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(text = count.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
