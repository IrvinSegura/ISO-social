package com.nexus.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Same look as IRSO BLOG: white and gray-950 backgrounds, primary buttons in gray-900
// (white in dark mode), amber for what should stand out, gray-200 borders around cards.

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Gray900,
    primaryContainer = Color(0xFF2B2310),
    onPrimaryContainer = Amber300,
    inversePrimary = Gray900,
    secondary = Amber400,
    onSecondary = Gray950,
    secondaryContainer = Color(0xFF2B2310),
    onSecondaryContainer = Amber300,
    tertiary = Emerald500,
    onTertiary = Gray950,
    background = Gray950,
    onBackground = Gray100,
    surface = Gray950,
    onSurface = Gray100,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Gray950,
    surfaceContainerLow = Color(0xFF0B1020),
    surfaceContainer = Gray900,
    surfaceContainerHigh = Gray800,
    surfaceContainerHighest = Gray700,
    outline = Gray700,
    outlineVariant = Gray800,
    error = Color(0xFFF87171),
    onError = Gray950,
    errorContainer = NexusErrorContainer,
    onErrorContainer = Color(0xFFFCA5A5),
    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = Gray900,
    onPrimary = Color.White,
    primaryContainer = Amber100,
    onPrimaryContainer = Amber800,
    inversePrimary = Amber300,
    secondary = Amber500,
    onSecondary = Gray950,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber800,
    tertiary = Emerald600,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Gray50,
    surfaceContainer = Gray50,
    surfaceContainerHigh = Gray100,
    surfaceContainerHighest = Gray200,
    outline = Gray300,
    outlineVariant = Gray200,
    error = Red600,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    inverseSurface = Gray900,
    inverseOnSurface = Gray50,
    scrim = Color.Black,
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

/**
 * Whether the theme being drawn is dark. Use it instead of isSystemInDarkTheme(): the user
 * can pick light or dark in the profile, and then the system setting no longer applies.
 */
@Composable
@ReadOnlyComposable
fun isNexusDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Amber used for highlights (badges, active tab text, accents), readable in both themes. */
val NexusAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isNexusDarkTheme()) Amber400 else Amber600
