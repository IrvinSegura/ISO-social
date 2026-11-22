package com.nexus.feature.profile.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.core.datastore.model.ThemeMode

private data class ThemeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)

private val themeOptions = listOf(
    ThemeOption(ThemeMode.SYSTEM, "Sistema", Icons.Outlined.BrightnessAuto),
    ThemeOption(ThemeMode.LIGHT, "Claro", Icons.Outlined.LightMode),
    ThemeOption(ThemeMode.DARK, "Oscuro", Icons.Outlined.DarkMode),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Ajustes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))
            SectionTitle("Apariencia")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = themeMode == option.mode,
                        onClick = { onThemeChange(option.mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = themeMode == option.mode) {
                                Icon(option.icon, contentDescription = null)
                            }
                        },
                    ) { Text(option.label) }
                }
            }
            Text(
                text = "«Sistema» sigue el modo claro u oscuro de tu teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Cuenta")
            ListItem(
                headlineContent = { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Tus publicaciones y guardados se quedan en tu cuenta") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    headlineColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = "Cerrar sesión") { confirmSignOut = true },
            )
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que volver a iniciar sesión para ver tu feed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}
