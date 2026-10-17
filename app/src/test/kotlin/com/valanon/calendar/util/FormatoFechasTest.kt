package com.valanon.calendar.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FormatoFechasTest {

    @Test
    fun `titulo de mes con inicial mayuscula y anio`() {
        assertEquals("Octubre 2026", FormatoFechas.tituloMes(YearMonth.of(2026, 10)))
        assertEquals("Enero 2027", FormatoFechas.tituloMes(YearMonth.of(2027, 1)))
    }

    @Test
    fun `titulo de dia como en la hoja de seleccion`() {
        assertEquals("Sábado, 17 de octubre", FormatoFechas.tituloDia(LocalDate.of(2026, 10, 17)))
        assertEquals("Miércoles, 4 de noviembre", FormatoFechas.tituloDia(LocalDate.of(2026, 11, 4)))
        assertEquals("Lunes, 1 de febrero", FormatoFechas.tituloDia(LocalDate.of(2027, 2, 1)))
    }

    @Test
    fun `dia y mes para la descripcion de accesibilidad`() {
        assertEquals("17 de octubre", FormatoFechas.diaYMes(LocalDate.of(2026, 10, 17)))
    }

    @Test
    fun `semana empieza en lunes`() {
        assertEquals(listOf("L", "M", "X", "J", "V", "S", "D"), FormatoFechas.INICIALES_SEMANA)
    }
}
