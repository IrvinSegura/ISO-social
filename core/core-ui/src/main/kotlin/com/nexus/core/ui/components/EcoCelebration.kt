package com.nexus.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.theme.NexusAccentTeal
import com.nexus.core.ui.theme.NexusPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DURATION_MS = 1500

/**
 * Micro-celebración para cuando el usuario publica un Eco — el gesto que en
 * Nexus reemplaza al "like". En lugar de un toast frío, el emblema de marca (✦)
 * nace con un resorte, irradia un destello y dispara chispas, luego se desvanece
 * solo y llama a [onFinished].
 *
 * Es un overlay a pantalla completa: colócalo como último hijo del contenedor de
 * la pantalla para que dibuje por encima de todo. Reutilizable desde el feed y
 * desde el detalle del post.
 *
 * @param visible al pasar a `true` reproduce la animación una vez.
 * @param onFinished se invoca al terminar; el llamador debe volver [visible] a `false`.
 */
@Composable
fun EcoCelebration(
    visible: Boolean,
    onFinished: () -> Unit,
    message: String = "Tu eco resuena",
) {
    if (!visible) return

    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(durationMillis = DURATION_MS, easing = LinearEasing))
        onFinished()
    }
    val p = anim.value

    // ── Curvas derivadas del progreso (0..1) ────────────────────────────────
    val enter = (p / 0.15f).coerceIn(0f, 1f)
    val exit = ((p - 0.78f) / 0.22f).coerceIn(0f, 1f)
    val overlayAlpha = enter * (1f - exit)

    val emblemRaw = (p / 0.45f).coerceIn(0f, 1f)
    val emblemScale = 0.2f + 0.8f * easeOutBack(emblemRaw)

    val ring = (p / 0.6f).coerceIn(0f, 1f)
    val ringEase = 1f - (1f - ring) * (1f - ring) // easeOutQuad
    val sparkleAlpha = ((1f - ring) * 1.2f).coerceIn(0f, 1f) * (1f - exit)

    val textAlpha = ((p - 0.25f) / 0.25f).coerceIn(0f, 1f) * (1f - exit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * overlayAlpha))
            // Consume toques para que nada de abajo reaccione durante la celebración
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                // Destello radial
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer { alpha = overlayAlpha * emblemRaw }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NexusPurple.copy(alpha = 0.45f),
                                    NexusAccentTeal.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                // Chispas que irradian
                repeat(8) { i ->
                    val angle = (2.0 * PI / 8.0) * i
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset {
                                val r = ringEase * 78.dp.toPx()
                                IntOffset(
                                    (cos(angle) * r).roundToInt(),
                                    (sin(angle) * r).roundToInt(),
                                )
                            }
                            .graphicsLayer { alpha = sparkleAlpha }
                            .clip(CircleShape)
                            .background(if (i % 2 == 0) NexusPurple else NexusAccentTeal),
                    )
                }

                // Emblema ✦
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            scaleX = emblemScale
                            scaleY = emblemScale
                            alpha = overlayAlpha
                        }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NexusPurple, NexusAccentTeal))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✦", fontSize = 38.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = message,
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Easing con un ligero rebote al final — da vida al nacimiento del emblema. */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}
