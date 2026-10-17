package com.valanon.calendar.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * Spanish date texts exactly as written in the spec. The names are hard-coded instead of
 * using the device locale so the UI and the PDF read the same on any phone.
 */
object FormatoFechas {

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    /** Index 0 = Monday, as java.time's DayOfWeek.value - 1. */
    private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

    /** Column headers of the month grid; the week starts on Monday. */
    val INICIALES_SEMANA = listOf("L", "M", "X", "J", "V", "S", "D")

    fun nombreMes(mes: Int): String = MESES[mes - 1]

    /** "Octubre 2026" */
    fun tituloMes(mes: YearMonth): String = "${nombreMes(mes.monthValue).capitalizada()} ${mes.year}"

    /** "Sábado, 17 de octubre" */
    fun tituloDia(fecha: LocalDate): String =
        "${DIAS[fecha.dayOfWeek.value - 1].capitalizada()}, ${diaYMes(fecha)}"

    /** "17 de octubre" */
    fun diaYMes(fecha: LocalDate): String = "${fecha.dayOfMonth} de ${nombreMes(fecha.monthValue)}"

    private fun String.capitalizada(): String = replaceFirstChar { it.uppercaseChar() }
}
