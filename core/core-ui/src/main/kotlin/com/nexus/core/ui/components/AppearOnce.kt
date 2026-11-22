package com.nexus.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * @param enabled     false muestra el contenido tal cual; útil para animar solo
 *                    la primera pantalla y no las tarjetas que llegan con el scroll.
 */
@Composable
fun AppearOnce(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideOffset: Dp = 24.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Saveable: a LazyColumn keeps it per item key, so a card scrolled away and back
    // does not fade in a second time
    var hasAppeared by rememberSaveable { mutableStateOf(!enabled) }
    val alpha = remember { Animatable(if (hasAppeared) 1f else 0f) }
    val offset = remember { Animatable(if (hasAppeared) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (hasAppeared) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        hasAppeared = true
        launch { alpha.animateTo(1f, tween(durationMillis = 380)) }
        offset.animateTo(
            0f,
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        )
    }

    Box(
        // Values are read inside the layer block: the animation only redraws the layer,
        // it never recomposes the card
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offset.value * slideOffset.toPx()
        },
    ) {
        content()
    }
}
