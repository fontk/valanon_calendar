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
    /** Ring of the current month in the month picker (tokens.json → todayRing). */
    val todayRing: Color,
    /** Bottom-sheet container, and the neutral "Cancelar" button inside it. */
    val sheet: Color,
    val sheetButton: Color,
    /** Outline of destructive buttons ("Eliminar plantilla"), tokens.json → dangerBorder. */
    val dangerBorder: Color,
    /** Turnos: background of a day with at least one assignment (tokens.json → dayWithData). */
    val dayWithData: Color,
    /** Turnos: dashed outline of the "Sin asignar" chip (tokens.json → unassignedBorder). */
    val unassignedBorder: Color,
    /** Badge of the "Tareas" template type, in Plantillas and Checklist (tokens.json → templateTasks*). */
    val templateTasksBg: Color,
    val templateTasksFg: Color,
    /** Icon of the 64 dp empty-state badge (Checklist without lists, Turnos without templates). */
    val emptyBadgeIcon: Color,
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
    todayRing = Color(0xFFB9CFCB),
    sheet = Color(0xFFFFFFFF),
    sheetButton = Color(0xFFF2F2EF),
    dangerBorder = Color(0xFFE6C9C3),
    dayWithData = Color(0xFFFAFAF8),
    unassignedBorder = Color(0xFFC9C9C4),
    templateTasksBg = Color(0xFFEDE9FF),
    templateTasksFg = Color(0xFF4B43B8),
    emptyBadgeIcon = Color(0xFF9A9A95),
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
    todayRing = Color(0x665DCAB9), // accentOnBackground at 40 % (DESIGN.md → SelectorMes)
    sheet = Color(0xFF1F1F1D), // "fondo de la hoja = superficie oscura"
    sheetButton = Color(0xFF2E2E2B), // stays visible on the dark sheet
    dangerBorder = Color(0xFF5E3129),
    dayWithData = Color(0xFF1F1F1D), // DESIGN.md: dark surface, one level above the background
    unassignedBorder = Color(0xFF55554F),
    templateTasksBg = Color(0xFF2A2650), // same violet, dark like surfaceAccent
    templateTasksFg = Color(0xFFB9B2FF),
    emptyBadgeIcon = Color(0xFF8F8F89),
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
    val TodayRing: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.todayRing
    val Sheet: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.sheet
    val SheetButton: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.sheetButton
    val DangerBorder: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.dangerBorder
    val DayWithData: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.dayWithData
    val UnassignedBorder: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.unassignedBorder
    val TemplateTasksBg: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.templateTasksBg
    val TemplateTasksFg: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.templateTasksFg
    val EmptyBadgeIcon: Color @Composable @ReadOnlyComposable get() = LocalPaleta.current.emptyBadgeIcon
}
