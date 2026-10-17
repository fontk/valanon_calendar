package com.valanon.calendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light-only theme built from the design tokens. Dynamic colour is deliberately off:
 * the accent and the five shift colours are part of the product identity.
 * (Dark theme is optional in DESIGN.md, planned for v1.1.)
 */
private val MisTurnosColorScheme = lightColorScheme(
    primary = MisTurnosColors.Accent,
    onPrimary = MisTurnosColors.OnAccent,
    primaryContainer = MisTurnosColors.SurfaceAccent,
    onPrimaryContainer = MisTurnosColors.AccentDark,
    secondary = MisTurnosColors.Accent,
    onSecondary = MisTurnosColors.OnAccent,
    secondaryContainer = MisTurnosColors.SurfaceAccent,
    onSecondaryContainer = MisTurnosColors.AccentDark,
    background = MisTurnosColors.Background,
    onBackground = MisTurnosColors.Text,
    surface = MisTurnosColors.Background,
    onSurface = MisTurnosColors.Text,
    surfaceVariant = MisTurnosColors.Surface,
    onSurfaceVariant = MisTurnosColors.TextMuted,
    surfaceContainerLowest = MisTurnosColors.Background,
    surfaceContainerLow = MisTurnosColors.Background,
    surfaceContainer = MisTurnosColors.Background,
    surfaceContainerHigh = MisTurnosColors.Background,
    surfaceContainerHighest = MisTurnosColors.Surface,
    outline = MisTurnosColors.Line,
    outlineVariant = MisTurnosColors.LineSoft,
    error = MisTurnosColors.Danger,
    scrim = MisTurnosColors.Scrim,
)

@Composable
fun MisTurnosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MisTurnosColorScheme,
        typography = Typography,
        content = content,
    )
}
