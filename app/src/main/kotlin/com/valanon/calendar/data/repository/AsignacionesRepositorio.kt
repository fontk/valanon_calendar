package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.AjusteDao
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.TipoTurno
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/**
 * Team shifts for the Turnos tab (spec 3b): which shift each worker of a "Trabajadores"
 * template has on each day, plus the active template stored in `ajustes`.
 * Never reads or writes `turnos` (the user's own shifts).
 */
class AsignacionesRepositorio(
    private val asignacionDao: AsignacionDao,
    private val ajusteDao: AjusteDao,
    plantillaDao: PlantillaDao,
    private val trabajadorDao: TrabajadorDao,
) {
    /** Templates the Turnos tab can show, in list order. */
    val plantillasDeTrabajadores: Flow<List<PlantillaConRecuento>> =
        plantillaDao.observarTodas().map { lista -> lista.filter { it.plantilla.tipo == TipoPlantilla.TRABAJADORES } }

    /**
     * The saved active template or, if none was saved or it was deleted, the first available
     * one (spec 3b.1). `null` when there is no "Trabajadores" template at all.
     */
    val plantillaActiva: Flow<PlantillaConRecuento?> =
        combine(plantillasDeTrabajadores, ajusteDao.observar(CLAVE_PLANTILLA_TURNOS)) { lista, guardada ->
            lista.firstOrNull { it.plantilla.id == guardada?.toLongOrNull() } ?: lista.firstOrNull()
        }

    fun trabajadores(plantillaId: Long): Flow<List<Trabajador>> = trabajadorDao.observarDePlantilla(plantillaId)

    /** Shifts of the template's workers in [mes], by day and worker id. */
    fun asignacionesMes(plantillaId: Long, mes: YearMonth): Flow<Map<LocalDate, Map<Long, TipoTurno>>> =
        asignacionDao.observarMes(plantillaId, mes.atDay(1).toString(), mes.atEndOfMonth().toString())
            .map { filas ->
                filas.groupBy({ LocalDate.parse(it.fecha) }, { it.trabajadorId to it.tipo })
                    .mapValues { (_, pares) -> pares.toMap() }
            }

    suspend fun elegirPlantilla(plantillaId: Long) =
        ajusteDao.guardar(Ajuste(CLAVE_PLANTILLA_TURNOS, plantillaId.toString()))

    /** `INSERT OR REPLACE`: saved immediately when picking a shift (no "Guardar" button). */
    suspend fun asignar(fecha: LocalDate, trabajadorId: Long, tipo: TipoTurno) =
        asignacionDao.upsert(Asignacion(fecha = fecha.toString(), trabajadorId = trabajadorId, tipo = tipo))

    /** "Quitar turno": the worker is left unassigned that day. */
    suspend fun quitar(fecha: LocalDate, trabajadorId: Long) = asignacionDao.borrar(fecha.toString(), trabajadorId)

    companion object {
        const val CLAVE_PLANTILLA_TURNOS = "turnos.plantilla_id"
    }
}
