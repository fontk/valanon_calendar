package com.valanon.calendar.util

/**
 * Layout of the team-shift dots in a Turnos day cell (spec 3b.2): how many 7 dp dots to draw
 * (at most [MAX_PUNTOS]) and the gap between them, so that the dots and the "+N" label fit in
 * [anchoDisponible] dp without clipping. The gap is 3 dp, or 2 dp when tight; if still too
 * narrow, fewer dots are drawn and "+N" counts the hidden ones.
 */
data class PuntosDia(val visibles: Int, val huecoDp: Float) {
    companion object {
        const val MAX_PUNTOS = 5
        const val DIAMETRO_DP = 7f

        /** Approximate width of "+N" at 9 sp. */
        const val ANCHO_EXTRA_DP = 10f

        fun calcular(total: Int, anchoDisponible: Float): PuntosDia {
            val maximo = minOf(total, MAX_PUNTOS)
            fun cabe(k: Int, hueco: Float): Boolean {
                val extra = if (k < total) hueco + ANCHO_EXTRA_DP else 0f
                return k * DIAMETRO_DP + (k - 1).coerceAtLeast(0) * hueco + extra <= anchoDisponible
            }
            val hueco = if (cabe(maximo, 3f)) 3f else 2f
            val visibles = (maximo downTo 0).first { cabe(it, hueco) }
            return PuntosDia(visibles, hueco)
        }
    }
}
