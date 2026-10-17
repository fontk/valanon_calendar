package com.valanon.calendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens from `mis-turnos-kit/design/tokens/tokens.json` → `color`, in a light and a
 * dark variant. The dark values follow DESIGN.md → "Tema oscuro"; tokens it does not list
 * are derived so text keeps ≥ 4.5:1 contrast on the dark background.
 */
@Immutable
data class PaletaMisTurnos(
    val oscuro: Boolean,
    /** Filled surfaces with white content: primary buttons, FAB. */
    val accent: Color,
    val accentDark: Color,
    val onAccent: Color,
    /** Accent used as text, icon or border on the background (today border, active tab, "Actual"). */
    val accentOnBackground: Color,
    val surfaceAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceInput: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val line: Color,
    val lineSoft: Color,
    val handle: Color,
    val danger: Color,
    val scrim: Color,
    val tabBarLine: Color,
    val emptyIcon: Color,
)

val PaletaClara = PaletaMisTurnos(
    oscuro = false,
    accent = Color(0xFF0F6E64),
    accentDark = Color(0xFF0B544C),
    onAccent = Color(0xFFFFFFFF),
    accentOnBackground = Color(0xFF0F6E64),
    surfaceAccent = Color(0xFFDDF0EC),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2EF),
    surfaceInput = Color(0xFFF7F7F4),
    text = Color(0xFF1C1C1A),
    textMuted = Color(0xFF5F5F5A),
    textFaint = Color(0xFF7A7A75),
    line = Color(0xFFE6E6E1),
    lineSoft = Color(0xFFE8E8E4),
    handle = Color(0xFFD8D8D3),
    danger = Color(0xFF8A2E1F),
    scrim = Color(0x73141413), // rgba(20, 20, 19, 0.45)
    tabBarLine = Color(0xFFECECE8),
    emptyIcon = Color(0xFFB9B9B3),
)

val PaletaOscura = PaletaMisTurnos(
    oscuro = true,
    accent = Color(0xFF0F6E64),
    accentDark = Color(0xFF0B544C),
    onAccent = Color(0xFFFFFFFF),
    accentOnBackground = Color(0xFF5DCAB9),
    surfaceAccent = Color(0xFF173A35),
    background = Color(0xFF141413), // DESIGN.md
    surface = Color(0xFF1F1F1D), // DESIGN.md
    surfaceInput = Color(0xFF1F1F1D),
    text = Color(0xFFF3F3EF), // DESIGN.md
    textMuted = Color(0xFFB4B4AE),
    textFaint = Color(0xFF8F8F89),
    line = Color(0xFF2E2E2B), // DESIGN.md
    lineSoft = Color(0xFF2E2E2B),
    handle = Color(0xFF4A4A46),
    danger = Color(0xFFE8907E),
    scrim = Color(0x99000000),
    tabBarLine = Color(0xFF2E2E2B),
    emptyIcon = Color(0xFF55554F),
)

val LocalPaleta = staticCompositionLocalOf { PaletaClara }

/**
 * Current palette for composables. Names match the token keys so the design docs can be
 * followed literally. Outside composition (e.g. the PDF) use [PaletaClara] directly.
 */
object MisTurnosColors {
    val EsOscuro: Boolean @Composable @ReadOnlyComposable get() = LocalPaleta.current.oscuro
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.accent
    val AccentDark: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.accentDark
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.onAccent
    val AccentOnBackground: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.accentOnBackground
    val SurfaceAccent: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.surfaceAccent
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.surface
    val SurfaceInput: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.surfaceInput
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.text
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.textMuted
    val TextFaint: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.textFaint
    val Line: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.line
    val LineSoft: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.lineSoft
    val Handle: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.handle
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.danger
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.scrim
    val TabBarLine: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.tabBarLine
    val EmptyIcon: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.emptyIcon
}
