package com.valanon.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App theme. Follows the system dark mode by default ([oscuro] = isSystemInDarkTheme()).
 * Dynamic colour is deliberately off: the accent and the five shift colours are part of the
 * product identity.
 */
@Composable
fun MisTurnosTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val paleta = if (oscuro) PaletaOscura else PaletaClara
    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(
            colorScheme = paleta.aColorScheme(),
            typography = Typography,
            content = content,
        )
    }
}

/** Maps the tokens onto Material 3 so stock components (snackbar, checkbox, sheet…) match. */
private fun PaletaMisTurnos.aColorScheme(): ColorScheme {
    val base = if (oscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = surfaceAccent,
        onPrimaryContainer = accentOnBackground,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceAccent,
        onSecondaryContainer = accentOnBackground,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = background,
        surfaceContainerLow = background,
        surfaceContainer = background,
        surfaceContainerHigh = background,
        surfaceContainerHighest = surface,
        outline = line,
        outlineVariant = lineSoft,
        // Snackbar: inverted surface with the accent for its action ("Deshacer").
        inverseSurface = text,
        inverseOnSurface = background,
        inversePrimary = if (oscuro) accent else surfaceAccent,
        error = danger,
        scrim = scrim,
    )
}
