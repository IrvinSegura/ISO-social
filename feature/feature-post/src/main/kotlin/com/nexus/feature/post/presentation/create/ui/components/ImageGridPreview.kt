package com.nexus.feature.post.presentation.create.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Plain rows instead of a lazy grid: the create screen scrolls vertically, and a lazy grid
 * inside it gets an unbounded height and crashes as soon as a photo is picked.
 */
@Composable
fun ImageGridPreview(
    uris: List<Uri>,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = if (uris.size == 1) 1 else 2
    val aspectRatio = if (uris.size == 1) 16f / 9f else 1f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        uris.chunked(columns).forEachIndexed { rowIndex, rowUris ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowUris.forEachIndexed { columnIndex, uri ->
                    val index = rowIndex * columns + columnIndex
                    ImageTile(
                        uri = uri,
                        index = index,
                        aspectRatio = aspectRatio,
                        onRemove = { onRemove(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keeps the last odd photo at half width, like the other cells
                repeat(columns - rowUris.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ImageTile(
    uri: Uri,
    index: Int,
    aspectRatio: Float,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(aspectRatio)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Imagen ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(26.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                ),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Quitar imagen",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
