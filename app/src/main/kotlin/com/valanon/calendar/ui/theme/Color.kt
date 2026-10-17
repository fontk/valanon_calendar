package com.valanon.calendar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from `mis-turnos-kit/design/tokens/tokens.json` → `color`.
 * Names match the token keys so the design docs can be followed literally.
 */
object MisTurnosColors {
    val Accent = Color(0xFF0F6E64)
    val AccentDark = Color(0xFF0B544C)
    val OnAccent = Color(0xFFFFFFFF)
    val SurfaceAccent = Color(0xFFDDF0EC)
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF2F2EF)
    val SurfaceInput = Color(0xFFF7F7F4)
    val Text = Color(0xFF1C1C1A)
    val TextMuted = Color(0xFF5F5F5A)
    val TextFaint = Color(0xFF7A7A75)
    val Line = Color(0xFFE6E6E1)
    val LineSoft = Color(0xFFE8E8E4)
    val Handle = Color(0xFFD8D8D3)
    val Danger = Color(0xFF8A2E1F)
    val Scrim = Color(0x73141413) // rgba(20, 20, 19, 0.45)

    /** Top border of the bottom bar in the mockups. */
    val TabBarLine = Color(0xFFECECE8)

    /** Empty-state icon grey (DESIGN.md → Checklist → estado vacío). */
    val EmptyIcon = Color(0xFFB9B9B3)
}
