package com.valanon.calendar.pdf

import com.valanon.calendar.data.local.entity.TipoTurno
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Builds the PDF of one month. Interface so ViewModels can be unit-tested without Android. */
fun interface GeneradorPdf {
    suspend fun exportar(mes: YearMonth, turnos: Map<LocalDate, TipoTurno>, tipos: List<TipoTurno>): File
}
