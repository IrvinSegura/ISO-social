package com.nexus.feature.post.presentation.detail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.NexusAsyncImage
import com.nexus.feature.post.domain.model.Eco
import com.nexus.feature.post.domain.model.EcoType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcoItem(
    eco: Eco,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = eco.authorName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(eco.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(6.dp))

        when (eco.type) {
            EcoType.TEXT -> {
                Text(
                    text = eco.message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            EcoType.EMOJI -> {
                Text(
                    text = eco.emoji ?: "😊",
                    fontSize = 40.sp,
                )
            }
            EcoType.GIF, EcoType.IMAGE -> {
                eco.mediaUrl?.let { url ->
                    NexusAsyncImage(
                        model = url,
                        contentDescription = if (eco.type == EcoType.GIF) "GIF" else "Imagen",
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1.5f),
                    )
                }
            }
            EcoType.AUDIO -> {
                Text(
                    text = "🎵 Nota de voz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("d MMM", Locale("es")).format(Date(timestamp))
    }
}
