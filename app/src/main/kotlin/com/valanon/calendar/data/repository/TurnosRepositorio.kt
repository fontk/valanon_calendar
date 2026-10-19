package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.TiposTurnoDao
import com.valanon.calendar.data.local.dao.TurnoDao
import com.valanon.calendar.data.local.entity.TipoTurno
import com.valanon.calendar.data.local.entity.Turno
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/**
 * The user's shifts by date. Translates between [LocalDate] and the ISO strings stored in
 * SQLite, and resolves `tipo_id` against `tipos_turno` so a renamed or recoloured type shows up
 * at once (both flows are observed).
 */
class TurnosRepositorio(private val dao: TurnoDao, private val tiposDao: TiposTurnoDao) {

    /** Configured shift types in order (legend and picker). */
    val tipos: Flow<List<TipoTurno>> = tiposDao.observarTodos()

    suspend fun obtenerTipos(): List<TipoTurno> = tiposDao.obtenerTodos()

    fun observarMes(mes: YearMonth): Flow<Map<LocalDate, TipoTurno>> =
        combine(
            dao.observarRango(desde = mes.atDay(1).toString(), hasta = mes.atEndOfMonth().toString()),
            tiposDao.observarTodos(),
        ) { turnos, tipos -> turnos.resolver(tipos) }

    suspend fun obtenerMes(mes: YearMonth): Map<LocalDate, TipoTurno> =
        dao.obtenerRango(desde = mes.atDay(1).toString(), hasta = mes.atEndOfMonth().toString())
            .resolver(tiposDao.obtenerTodos())

    suspend fun asignar(fecha: LocalDate, tipo: TipoTurno) =
        dao.upsert(Turno(fecha = fecha.toString(), tipoId = tipo.id))

    suspend fun quitar(fecha: LocalDate) = dao.borrar(fecha.toString())
}

/**
 * Day → type. A row whose type is missing cannot exist (ON DELETE CASCADE), but a read racing a
 * delete is tolerated by skipping it instead of failing.
 */
private fun List<Turno>.resolver(tipos: List<TipoTurno>): Map<LocalDate, TipoTurno> {
    val porId = tipos.associateBy { it.id }
    return mapNotNull { turno -> porId[turno.tipoId]?.let { LocalDate.parse(turno.fecha) to it } }.toMap()
}
