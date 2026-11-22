package com.nexus.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
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
    // AsyncImage instead of SubcomposeAsyncImage: subcomposition per image is costly
    // in a scrolling feed. The loading and error states are drawn around it instead
    var state by remember(model) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val request = remember(model) {
        ImageRequest.Builder(context).data(model).crossfade(true).build()
    }
    val isLoading = state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Empty

    // propagateMinConstraints: the image takes the size the caller gave this Box, as it did
    // when the modifier was applied to the image itself
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onState = { state = it },
            modifier = if (isLoading) Modifier.background(rememberShimmerBrush()) else Modifier,
        )
        if (state is AsyncImagePainter.State.Error || model == null) {
            Fallback(initial = fallbackInitial, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun Fallback(initial: String?, modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!initial.isNullOrBlank()) {
            Text(
                text = initial.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
