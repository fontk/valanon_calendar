package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.TipoTurno
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** Shifts by date. Translates between [LocalDate] and the ISO strings stored in SQLite. */
class TurnosRepositorio(private val dao: TurnoDao) {

    fun observarMes(mes: YearMonth): Flow<Map<LocalDate, TipoTurno>> =
        dao.observarRango(desde = mes.atDay(1).toString(), hasta = mes.atEndOfMonth().toString())
            .map { it.aMapa() }

    suspend fun obtenerMes(mes: YearMonth): Map<LocalDate, TipoTurno> =
        dao.obtenerRango(desde = mes.atDay(1).toString(), hasta = mes.atEndOfMonth().toString())
            .aMapa()

    suspend fun asignar(fecha: LocalDate, tipo: TipoTurno) =
        dao.upsert(Turno(fecha = fecha.toString(), tipo = tipo))

    suspend fun quitar(fecha: LocalDate) = dao.borrar(fecha.toString())

    private fun List<Turno>.aMapa(): Map<LocalDate, TipoTurno> =
        associate { LocalDate.parse(it.fecha) to it.tipo }
}
