package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntosDiaTest {

    /** Width actually used by the layout, to check nothing overflows. */
    private fun ancho(d: PuntosDia, total: Int): Float {
        val k = d.visibles
        val extra = if (k < total) d.huecoDp + PuntosDia.ANCHO_EXTRA_DP else 0f
        return k * PuntosDia.DIAMETRO_DP + (k - 1).coerceAtLeast(0) * d.huecoDp + extra
    }

    @Test
    fun `con sitio de sobra dibuja todos hasta cinco con hueco de 3`() {
        assertEquals(PuntosDia(4, 3f), PuntosDia.calcular(total = 4, anchoDisponible = 80f))
        assertEquals(PuntosDia(5, 3f), PuntosDia.calcular(total = 5, anchoDisponible = 80f))
    }

    @Test
    fun `mas de cinco deja sitio para +N`() {
        val d = PuntosDia.calcular(total = 7, anchoDisponible = 80f)
        assertEquals(5, d.visibles)
        assertTrue(ancho(d, 7) <= 80f)
    }

    @Test
    fun `en celdas estrechas reduce el hueco y luego los puntos sin desbordar`() {
        // 45 dp: five dots fit only with a 2 dp gap.
        assertEquals(PuntosDia(5, 2f), PuntosDia.calcular(total = 5, anchoDisponible = 45f))
        // 41 dp with 7 workers: fewer dots plus "+N", never wider than the cell.
        val d = PuntosDia.calcular(total = 7, anchoDisponible = 41f)
        assertTrue(d.visibles in 1..4)
        assertTrue(ancho(d, 7) <= 41f)
    }

    @Test
    fun `sin asignados no dibuja nada`() {
        assertEquals(0, PuntosDia.calcular(total = 0, anchoDisponible = 40f).visibles)
    }
}
