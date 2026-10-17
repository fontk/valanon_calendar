package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarioMesTest {

    @Test
    fun `octubre 2026 empieza en jueves y deja tres huecos`() {
        val celdas = CalendarioMes.celdas(YearMonth.of(2026, 10))

        assertEquals(listOf(null, null, null), celdas.take(3))
        assertEquals(LocalDate.of(2026, 10, 1), celdas[3])
        assertEquals(3 + 31, celdas.size)
        assertEquals(LocalDate.of(2026, 10, 31), celdas.last())
    }

    @Test
    fun `mes que empieza en lunes no tiene huecos`() {
        val celdas = CalendarioMes.celdas(YearMonth.of(2027, 2))

        assertEquals(LocalDate.of(2027, 2, 1), celdas.first())
        assertEquals(28, celdas.size)
        assertEquals(4, CalendarioMes.semanas(YearMonth.of(2027, 2)).size)
    }

    @Test
    fun `mes que empieza en domingo ocupa seis semanas`() {
        val semanas = CalendarioMes.semanas(YearMonth.of(2026, 3))

        assertEquals(List(6) { null }, semanas.first().take(6))
        assertEquals(LocalDate.of(2026, 3, 1), semanas.first()[6])
        assertEquals(6, semanas.size)
        assertTrue(semanas.all { it.size == 7 })
    }

    @Test
    fun `febrero bisiesto tiene 29 dias`() {
        val dias = CalendarioMes.celdas(YearMonth.of(2028, 2)).filterNotNull()
        assertEquals(29, dias.size)
    }
}
