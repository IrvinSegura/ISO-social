package com.nexus.feature.feed.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.core.ui.components.shimmer

/**
 * Esqueleto de carga del feed. En lugar de un spinner genérico, muestra
 * tarjetas "fantasma" que replican la silueta del [PostCard] real con un
 * shimmer suave — la transición a contenido real se siente fluida y la
 * percepción de velocidad mejora notablemente.
 */
@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        StoriesRowSkeleton()
        repeat(3) { index ->
            // Cada tarjeta tiene un cuerpo de altura ligeramente distinta para
            // que el conjunto no se vea robótico.
            SkeletonPostCard(contentLines = if (index % 2 == 0) 3 else 2)
        }
    }
}

@Composable
private fun StoriesRowSkeleton() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(5) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).shimmer(CircleShape))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(width = 40.dp, height = 9.dp).shimmer())
            }
        }
    }
}

@Composable
private fun SkeletonPostCard(contentLines: Int) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: avatar + nombre + meta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).shimmer(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    ShimmerLine(width = 130.dp, height = 13.dp)
                    Spacer(Modifier.height(7.dp))
                    ShimmerLine(width = 80.dp, height = 10.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cuerpo: líneas de texto, la última más corta
            repeat(contentLines) { line ->
                val isLast = line == contentLines - 1
                ShimmerLine(
                    width = if (isLast) 180.dp else null,
                    height = 12.dp,
                )
                if (!isLast) Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(18.dp))

            // Barra de reacciones fantasma
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    Box(Modifier.size(32.dp).shimmer(CircleShape))
                }
            }
        }
    }
}

/** Línea de shimmer. [width] nulo = ocupa todo el ancho disponible. */
@Composable
private fun ShimmerLine(width: Dp?, height: Dp) {
    val base = Modifier.height(height)
    val sized = if (width != null) base.width(width) else base.fillMaxWidth()
    Box(sized.shimmer(RoundedCornerShape(6.dp)))
}
