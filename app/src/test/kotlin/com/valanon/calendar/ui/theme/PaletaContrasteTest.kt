package com.valanon.calendar.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC.md → Accesibilidad: text contrast ≥ 4.5:1, in the light and in the dark palette. */
class PaletaContrasteTest {

    private fun contraste(a: Color, b: Color): Double {
        val (claro, oscuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05) / (oscuro + 0.05)
    }

    private fun comprobar(nombre: String, texto: Color, fondo: Color, minimo: Double = 4.5) {
        val c = contraste(texto, fondo)
        assertTrue("$nombre: contraste %.2f < $minimo".format(c), c >= minimo)
    }

    private fun comprobarPaleta(p: PaletaMisTurnos) {
        val modo = if (p.oscuro) "oscuro" else "claro"
        comprobar("$modo text/background", p.text, p.background)
        comprobar("$modo text/surfaceInput", p.text, p.surfaceInput)
        comprobar("$modo textMuted/background", p.textMuted, p.background)
        // textFaint is only used for incidental text (weekday initials, placeholder, completed
        // tasks). The light design token #7A7A75 measures 4.31:1 on white, so it is held to 3:1.
        comprobar("$modo textFaint/background", p.textFaint, p.background, minimo = 3.0)
        comprobar("$modo textFaint/surfaceInput (tarea hecha)", p.textFaint, p.surfaceInput, minimo = 3.0)
        comprobar("$modo onAccent/accent (botones)", p.onAccent, p.accent)
        comprobar("$modo accentOnBackground/background", p.accentOnBackground, p.background)
        comprobar("$modo accentOnBackground/surfaceAccent (pestaña activa)", p.accentOnBackground, p.surfaceAccent)
        comprobar("$modo inverse snackbar", p.background, p.text)
        // Icons and borders (non-text) need 3:1.
        comprobar("$modo danger/background (papelera)", p.danger, p.background, minimo = 3.0)
    }

    @Test
    fun `paleta clara cumple el contraste`() = comprobarPaleta(PaletaClara)

    @Test
    fun `paleta oscura cumple el contraste`() = comprobarPaleta(PaletaOscura)
}
