package com.nexus.feature.circles.presentation.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.core.ui.components.CircleBadge
import com.nexus.core.ui.components.NexusCard
import com.nexus.core.ui.components.NexusPrimaryButton
import com.nexus.core.ui.theme.CirclePalette
import com.nexus.feature.circles.domain.model.InviteCode
import com.nexus.feature.circles.presentation.list.CirclesViewModel
import com.nexus.feature.circles.presentation.list.JoinState

val CircleEmojis = listOf(
    "⭕", "🏠", "👨‍👩‍👧", "🤝", "🎓", "💼", "🎮", "🎵",
    "📚", "☕", "⚽", "🏃", "🎨", "🍳", "✈️", "🌿",
)

private val ColorNames = listOf("Ámbar", "Índigo", "Esmeralda", "Rosa", "Cielo", "Violeta", "Naranja", "Turquesa")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCircleSheet(
    isCreating: Boolean,
    onCreate: (name: String, emoji: String, colorIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(CircleEmojis[1]) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    val canCreate = name.isNotBlank() && !isCreating

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Crea tu círculo", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBadge(emoji = emoji, colorIndex = colorIndex, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= CirclesViewModel.MAX_NAME) name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Familia, Equipo, Amigos…") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    supportingText = { Text("${name.length}/${CirclesViewModel.MAX_NAME}") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Ícono")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircleEmojis.forEach { option ->
                    val selected = option == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .border(
                                BorderStroke(
                                    if (selected) 2.dp else 0.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable(role = Role.RadioButton) { emoji = option }
                            .semantics { this.selected = selected },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option, fontSize = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CirclePalette.forEachIndexed { index, color ->
                    val selected = index == colorIndex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable(role = Role.RadioButton) { colorIndex = index }
                            .semantics {
                                this.selected = selected
                                contentDescription = ColorNames[index]
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            NexusPrimaryButton(
                text = if (isCreating) "Creando…" else "Crear círculo",
                onClick = { onCreate(name, emoji, colorIndex) },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Te daremos un código para invitar a quien quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCircleSheet(
    join: JoinState,
    onLookUp: (String) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Únete con un código", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pídele el código a alguien del círculo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            val preview = join.preview
            if (preview == null) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        code = InviteCode.normalize(input).take(InviteCode.LENGTH)
                        if (join.error != null) onReset()
                    },
                    label = { Text("Código") },
                    placeholder = { Text("ABCD2345") },
                    singleLine = true,
                    isError = join.error != null,
                    supportingText = join.error?.let { { Text(it) } },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onLookUp(code) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                NexusPrimaryButton(
                    text = if (join.isSearching) "Buscando…" else "Buscar círculo",
                    onClick = { onLookUp(code) },
                    enabled = code.length == InviteCode.LENGTH && !join.isSearching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            } else {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleBadge(emoji = preview.emoji, colorIndex = preview.colorIndex, size = 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(preview.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Código ${InviteCode.format(preview.code)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                join.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                NexusPrimaryButton(
                    text = if (join.isJoining) "Uniéndote…" else "Unirme a ${preview.name}",
                    onClick = onConfirm,
                    enabled = !join.isJoining,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
                TextButton(onClick = onReset, enabled = !join.isJoining, modifier = Modifier.fillMaxWidth()) {
                    Text("Usar otro código")
                }
            }
            if (join.isSearching || join.isJoining) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterHorizontally),
                    strokeWidth = 2.dp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
