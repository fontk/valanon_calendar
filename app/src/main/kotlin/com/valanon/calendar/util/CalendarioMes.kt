package com.valanon.calendar.util

import java.time.LocalDate
import java.time.YearMonth

object CalendarioMes {

    /**
     * Cells of the month grid, Monday first: `null` for the blank cells before day 1,
     * then one [LocalDate] per day. No trailing blanks.
     */
    fun celdas(mes: YearMonth): List<LocalDate?> {
        val primerDia = mes.atDay(1)
        val huecos = primerDia.dayOfWeek.value - 1
        return List(huecos) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    }

    /** [celdas] split into weeks of 7, the last week padded with `null`. */
    fun semanas(mes: YearMonth): List<List<LocalDate?>> =
        celdas(mes).chunked(7) { semana -> semana + List(7 - semana.size) { null } }
}
