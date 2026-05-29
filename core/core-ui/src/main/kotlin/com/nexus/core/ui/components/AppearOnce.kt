package com.nexus.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Anima la primera aparición de su contenido: un fade-in combinado con un
 * sutil desplazamiento hacia arriba, gobernado por un resorte para que se
 * sienta vivo y no mecánico.
 *
 * Pensado para elementos de listas: al envolver cada tarjeta del feed con un
 * [delayMillis] escalonado según su índice, la lista "asciende" en cascada al
 * cargar. Reutilizable en cualquier feature.
 *
 * @param delayMillis retraso antes de animar; úsalo escalonado por índice para
 *                    el efecto cascada (conviene acotarlo, p. ej. a 200 ms).
 */
@Composable
fun AppearOnce(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideOffset: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 380),
        label = "appear_alpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else slideOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "appear_offset",
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY.toPx()
        },
    ) {
        content()
    }
}
