package com.nexus.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Imagen remota de Nexus con estados de carga y error de primera clase.
 *
 * Mientras descarga muestra un [rememberShimmerBrush] (no un cuadro gris que
 * hace "pop"); al terminar hace crossfade; si falla cae a un fondo neutro con
 * una inicial opcional. Es el componente de imagen canónico del design system:
 * cualquier feature (feed, perfil, círculos, chat) lo reutiliza y obtiene el
 * mismo comportamiento pulido sin repetir lógica.
 *
 * @param fallbackInitial letra a mostrar si la imagen falla o el modelo es nulo
 *                        (p. ej. la inicial del nombre para avatares).
 */
@Composable
fun NexusAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
    fallbackInitial: String? = null,
) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
        loading = {
            Box(Modifier.fillMaxSize().background(rememberShimmerBrush()))
        },
        error = {
            Fallback(initial = fallbackInitial)
        },
    )
}

@Composable
private fun Fallback(initial: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!initial.isNullOrBlank()) {
            androidx.compose.material3.Text(
                text = initial.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
