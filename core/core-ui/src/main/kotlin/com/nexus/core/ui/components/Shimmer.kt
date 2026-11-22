package com.nexus.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nexus.core.ui.theme.isNexusDarkTheme
import com.nexus.core.ui.theme.NexusPurple

/**
 * Pincel animado para placeholders de carga (skeleton screens).
 *
 * Pinta un gradiente diagonal que se desplaza en bucle, dando la sensación de
 * que el contenido "respira" mientras llega. Es neutro pero con un guiño sutil
 * a la marca (NexusPurple) para no romper la identidad visual.
 *
 * Diseñado para reutilizarse en cualquier feature: feed, perfil, círculos…
 *
 * Uso:
 * ```
 * Box(Modifier.size(48.dp).clip(CircleShape).background(rememberShimmerBrush()))
 * // o, más cómodo:
 * Box(Modifier.size(48.dp).shimmer(CircleShape))
 * ```
 */
@Composable
fun rememberShimmerBrush(durationMillis: Int = 1200): Brush {
    val isDark = isNexusDarkTheme()
    val base = MaterialTheme.colorScheme.surfaceVariant

    // Valle (más tenue) → cresta (con un toque de marca) → valle.
    val shimmerColors = listOf(
        base.copy(alpha = if (isDark) 0.45f else 0.65f),
        NexusPurple.copy(alpha = if (isDark) 0.18f else 0.10f),
        base.copy(alpha = if (isDark) 0.45f else 0.65f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translate - 500f, translate - 500f),
        end = Offset(translate, translate),
    )
}

/**
 * Atajo para convertir cualquier caja en un bloque "fantasma" con shimmer.
 * Recorta a [shape] y aplica el pincel animado de [rememberShimmerBrush].
 */
@Composable
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier =
    this
        .clip(shape)
        .background(rememberShimmerBrush())
