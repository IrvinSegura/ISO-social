package com.nexus.core.giphy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GifBox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nexus.core.giphy.GiphyItem
import com.nexus.core.giphy.GiphyKind
import com.nexus.core.ui.components.NexusEmptyState

/**
 * Bottom sheet to pick a GIF or an animated emoji from Giphy. It keeps its own state, so
 * a screen only shows it and receives the URL of what was picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiphyPickerSheet(
    onPicked: (url: String) -> Unit,
    onDismiss: () -> Unit,
    initialKind: GiphyKind = GiphyKind.GIF,
    viewModel: GiphyPickerViewModel = hiltViewModel(key = "giphy-picker"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialKind) { viewModel.onKindChanged(initialKind) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text("GIFs y emojis", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))

            KindTabs(selected = state.kind, onSelect = viewModel::onKindChanged)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text(if (state.kind == GiphyKind.GIF) "Buscar GIFs" else "Buscar emojis") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                enabled = state.isAvailable,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
            ) {
                when {
                    !state.isAvailable -> NexusEmptyState(
                        icon = Icons.Outlined.GifBox,
                        title = "GIFs no disponibles",
                        message = "Esta versión de la app no tiene la clave de Giphy.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.isLoading && state.items.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.hasError -> NexusEmptyState(
                        icon = Icons.Outlined.WifiOff,
                        title = "No se pudieron cargar",
                        message = "Revisa tu conexión e inténtalo de nuevo.",
                        actionLabel = "Reintentar",
                        onAction = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.items.isEmpty() -> Text(
                        text = "Sin resultados para \"${state.query.trim()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> ResultsGrid(kind = state.kind, items = state.items, onPicked = onPicked)
                }
            }

            // Giphy's terms ask for attribution wherever its results are shown
            Text(
                text = "Con tecnología de GIPHY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun KindTabs(selected: GiphyKind, onSelect: (GiphyKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(GiphyKind.GIF to "GIF", GiphyKind.EMOJI to "Emoji").forEach { (kind, label) ->
            val isSelected = kind == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Tab) { onSelect(kind) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultsGrid(kind: GiphyKind, items: List<GiphyItem>, onPicked: (String) -> Unit) {
    val isEmoji = kind == GiphyKind.EMOJI
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isEmoji) 4 else 3),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            AsyncImage(
                model = item.previewUrl,
                contentDescription = item.title.ifBlank { if (isEmoji) "Emoji" else "GIF" },
                // Emoji are transparent: shown whole, without a tile behind them
                contentScale = if (isEmoji) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (isEmoji) Modifier else Modifier.background(MaterialTheme.colorScheme.surfaceVariant))
                    .clickable { onPicked(item.url) }
                    .padding(if (isEmoji) 4.dp else 0.dp),
            )
        }
    }
}
