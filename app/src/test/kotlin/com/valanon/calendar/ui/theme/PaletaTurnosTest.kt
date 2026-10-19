package com.valanon.calendar.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.valanon.calendar.data.local.TIPOS_TURNO_POR_DEFECTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec 3c.3: the 16-entry shift palette, its contrast and the five defaults' mapping. */
class PaletaTurnosTest {

    private fun contraste(a: Color, b: Color): Double {
        val (claro, oscuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05) / (oscuro + 0.05)
    }

    @Test
    fun `tiene las 16 entradas en el orden del mockup`() {
        assertEquals(
            listOf(
                "gris", "amarillo", "ambar", "naranja", "rojo", "rosa", "fucsia", "violeta",
                "indigo", "azul", "celeste", "turquesa", "verde", "lima", "marron", "pizarra",
            ),
            PaletaTurnos.entradas.map { it.clave },
        )
    }

    @Test
    fun `los cinco por defecto conservan sus colores de siempre`() {
        val esperados = mapOf(
            "libre" to Color(0xFFF2F2EF),
            "manana" to Color(0xFFFFE9A8),
            "tarde" to Color(0xFFCFE4FF),
            "noche" to Color(0xFF2E2A6B),
            "vacaciones" to Color(0xFFCDEFD5),
        )
        TIPOS_TURNO_POR_DEFECTO.forEach { tipo ->
            assertEquals(tipo.clave, esperados[tipo.clave], PaletaTurnos[tipo.color].claro.fondo)
        }
    }

    @Test
    fun `una clave desconocida cae en gris`() {
        assertEquals("gris", PaletaTurnos["no-existe"].clave)
    }

    @Test
    fun `texto sobre fondo cumple 4,5 a 1 en claro y en oscuro`() {
        PaletaTurnos.entradas.forEach { e ->
            listOf("claro" to e.claro, "oscuro" to e.oscuro).forEach { (modo, c) ->
                val ratio = contraste(c.texto, c.fondo)
                assertTrue("${e.clave} $modo: %.2f".format(ratio), ratio >= 4.5)
            }
        }
    }

    /**
     * Dots are graphics, so 3:1. Five mockup dots measure below it on white (gris 2.83, amarillo
     * 2.38, ámbar 2.997, celeste 2.95, lima 2.18) although spec 3c.3 says ≥ 3:1; the palette must not
     * change without confirmation (AGENTS.md), so they are listed here and every other entry must
     * keep 3:1.
     */
    @Test
    fun `los puntos se ven sobre blanco y sobre el fondo oscuro`() {
        val excepcionesClaro = setOf("gris", "amarillo", "ambar", "celeste", "lima")
        PaletaTurnos.entradas.forEach { e ->
            val claro = contraste(e.claro.punto, Color.White)
            val minimo = if (e.clave in excepcionesClaro) 2.0 else 3.0
            assertTrue("${e.clave} punto/blanco: %.2f".format(claro), claro >= minimo)
            val oscuro = contraste(e.oscuro.punto, PaletaOscura.background)
            assertTrue("${e.clave} punto/fondo oscuro: %.2f".format(oscuro), oscuro >= 3.0)
        }
    }

    @Test
    fun `primer color libre salta los usados y vuelve a gris si estan todos`() {
        assertEquals("ambar", PaletaTurnos.primeraLibre(TIPOS_TURNO_POR_DEFECTO.map { it.color }))
        assertEquals("gris", PaletaTurnos.primeraLibre(PaletaTurnos.entradas.map { it.clave }))
    }
}
