package com.nexus.feature.post.presentation.create.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.feature.post.presentation.create.RecordingState

@Composable
fun AudioRecorderPanel(
    recordingState: RecordingState,
    durationMs: Long,
    onStop: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (recordingState) {
        RecordingState.Recording -> MaterialTheme.colorScheme.errorContainer
        RecordingState.Recorded -> MaterialTheme.colorScheme.primaryContainer
        RecordingState.Idle -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Pulsing mic icon while recording
            val micTint = if (recordingState == RecordingState.Recording) {
                val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                val color by infiniteTransition.animateColor(
                    initialValue = MaterialTheme.colorScheme.error,
                    targetValue = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "mic_color",
                )
                color
            } else {
                MaterialTheme.colorScheme.primary
            }

            Icon(
                Icons.Outlined.Mic,
                contentDescription = null,
                tint = micTint,
                modifier = Modifier.size(22.dp),
            )

            Text(
                text = when (recordingState) {
                    RecordingState.Recording -> "● ${formatDuration(durationMs)}"
                    RecordingState.Recorded -> "Audio grabado (${formatDuration(durationMs)})"
                    RecordingState.Idle -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )

            if (recordingState == RecordingState.Recording) {
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Stop, contentDescription = "Detener", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (recordingState == RecordingState.Recorded) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar audio", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
