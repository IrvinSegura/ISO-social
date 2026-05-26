package com.nexus.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NexusPurple,
    onPrimary = NexusLightBg,
    primaryContainer = NexusPurpleDark,
    onPrimaryContainer = NexusPurpleLight,
    secondary = NexusAccentTeal,
    onSecondary = NexusDarkBg,
    secondaryContainer = NexusAccentTealDark,
    onSecondaryContainer = NexusDarkOnSurface,
    background = NexusDarkBg,
    onBackground = NexusDarkOnSurface,
    surface = NexusDarkSurface,
    onSurface = NexusDarkOnSurface,
    surfaceVariant = NexusDarkSurfaceVariant,
    onSurfaceVariant = NexusDarkOnSurfaceVariant,
    error = NexusError,
    errorContainer = NexusErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = NexusPurple,
    onPrimary = NexusLightSurface,
    primaryContainer = NexusLightSurfaceVariant,
    onPrimaryContainer = NexusPurpleDark,
    secondary = NexusAccentTeal,
    onSecondary = NexusDarkBg,
    secondaryContainer = NexusAccentTealDark,
    onSecondaryContainer = NexusLightOnSurface,
    background = NexusLightBg,
    onBackground = NexusLightOnSurface,
    surface = NexusLightSurface,
    onSurface = NexusLightOnSurface,
    surfaceVariant = NexusLightSurfaceVariant,
    onSurfaceVariant = NexusLightOnSurfaceVariant,
    error = NexusError,
)

@Composable
fun NexusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexusTypography,
        shapes = NexusShapes,
        content = content,
    )
}
