package com.nexus.feature.post.presentation.detail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.theme.Amber500
import com.nexus.core.ui.theme.Gray900
import com.nexus.feature.post.domain.model.EcoReplyTarget

/** One-tap emoji ecos above the field. */
private val QUICK_ECO_EMOJIS = listOf("❤️", "🔥", "👏", "🙌", "😂", "🤔", "💡", "🌱")

private const val MAX_ECO_LENGTH = 500

/**
 * The eco bar pinned under the post, chat style: who it answers (when replying), a row of
 * one-tap emoji, and a rounded field with GIF and photo next to the send button.
 */
@Composable
fun EcoComposer(
    text: String,
    onTextChange: (String) -> Unit,
    replyingTo: EcoReplyTarget?,
    isSending: Boolean,
    focusRequester: FocusRequester,
    onSendText: () -> Unit,
    onSendEmoji: (String) -> Unit,
    onPickGif: () -> Unit,
    onPickImage: () -> Unit,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Opening a reply puts the cursor in the field, ready to type
    LaunchedEffect(replyingTo) {
        if (replyingTo != null) runCatching { focusRequester.requestFocus() }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AnimatedVisibility(
                visible = replyingTo != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ReplyBanner(name = replyingTo?.name.orEmpty(), onCancel = onCancelReply)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                QUICK_ECO_EMOJIS.forEach { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(
                                enabled = !isSending,
                                role = Role.Button,
                                onClickLabel = "Enviar eco $emoji",
                            ) { onSendEmoji(emoji) },
                    ) { Text(emoji, fontSize = 22.sp) }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(start = 16.dp, end = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = replyingTo?.let { "Responde a ${it.name}…" } ?: "Escribe un eco…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { if (it.length <= MAX_ECO_LENGTH) onTextChange(it) },
                            enabled = !isSending,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(Amber500),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                    }
                    AnimatedVisibility(visible = text.isEmpty()) {
                        Row {
                            IconButton(onClick = onPickGif, enabled = !isSending) {
                                Icon(Icons.Outlined.Gif, contentDescription = "Eco con GIF", modifier = Modifier.size(28.dp))
                            }
                            IconButton(onClick = onPickImage, enabled = !isSending) {
                                Icon(Icons.Outlined.Image, contentDescription = "Eco con foto")
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                SendButton(
                    enabled = text.isNotBlank() && !isSending,
                    sending = isSending,
                    onClick = onSendText,
                )
            }
        }
    }
}

@Composable
private fun ReplyBanner(name: String, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Amber500.copy(alpha = 0.10f))
            .padding(start = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 20.dp)
                .clip(CircleShape)
                .background(Amber500),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append("Respondiendo a ")
                withStyle(SpanStyle(color = Amber500, fontWeight = FontWeight.SemiBold)) { append("@$name") }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancelar respuesta", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, sending: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (enabled || sending) Amber500 else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "sendColor",
    )
    val scale by animateFloatAsState(if (enabled) 1f else 0.9f, label = "sendScale")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Enviar eco", onClick = onClick),
    ) {
        if (sending) {
            CircularProgressIndicator(color = Gray900, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar eco",
                tint = if (enabled) Gray900 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
