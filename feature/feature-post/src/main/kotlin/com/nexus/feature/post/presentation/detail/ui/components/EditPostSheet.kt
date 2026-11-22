package com.nexus.feature.post.presentation.detail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nexus.feature.post.domain.usecase.UpdatePostUseCase
import com.nexus.feature.post.presentation.create.ui.components.MoodPickerSheet

private const val MAX_LENGTH = UpdatePostUseCase.MAX_CONTENT_LENGTH

/** Lets the author change the text and mood of a post; media stays as it was published. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostSheet(
    initialContent: String,
    initialMood: String?,
    isSaving: Boolean,
    onSave: (content: String, mood: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent, selection = TextRange(initialContent.length)))
    }
    var mood by rememberSaveable { mutableStateOf(initialMood) }
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val trimmed = text.text.trim()
    val changed = trimmed != initialContent.trim() || mood != initialMood
    val canSave = trimmed.isNotEmpty() && changed && !isSaving

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Editar publicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    text = if (value.text.length <= MAX_LENGTH) value
                    else value.copy(text = value.text.take(MAX_LENGTH), selection = TextRange(MAX_LENGTH))
                },
                placeholder = { Text("¿Qué quieres decir?") },
                minLines = 4,
                maxLines = 10,
                shape = RoundedCornerShape(18.dp),
                isError = trimmed.isEmpty(),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (trimmed.isEmpty()) "El texto no puede quedar vacío" else "Las fotos y archivos no cambian")
                        Text(
                            "${text.text.length}/$MAX_LENGTH",
                            color = if (text.text.length >= MAX_LENGTH) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(8.dp))
            val currentMood = mood
            if (currentMood == null) {
                AssistChip(
                    onClick = { showMoodPicker = true },
                    label = { Text("Añadir estado de ánimo") },
                    leadingIcon = { Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InputChip(
                        selected = true,
                        onClick = { showMoodPicker = true },
                        label = { Text(currentMood) },
                    )
                    TextButton(onClick = { mood = null }) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Quitar")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSave(trimmed, mood) },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showMoodPicker) {
        MoodPickerSheet(
            selectedMood = mood,
            onMoodSelected = {
                mood = it
                showMoodPicker = false
            },
            onDismiss = { showMoodPicker = false },
        )
    }
}
