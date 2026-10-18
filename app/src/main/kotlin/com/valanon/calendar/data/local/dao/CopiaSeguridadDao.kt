package com.valanon.calendar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.Tarea
import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.local.entity.Turno
import com.valanon.calendar.data.model.DatosApp

/**
 * Whole-database read and replace for the backup file (export/import). Both run in a single
 * transaction: the export is a consistent snapshot and the import is all-or-nothing.
 */
@Dao
abstract class CopiaSeguridadDao {

    @Query("SELECT * FROM turnos ORDER BY fecha")
    protected abstract suspend fun turnos(): List<Turno>

    @Query("SELECT * FROM listas ORDER BY orden, id")
    protected abstract suspend fun listas(): List<Lista>

    @Query("SELECT * FROM tareas ORDER BY lista_id, orden, id")
    protected abstract suspend fun tareas(): List<Tarea>

    @Query("SELECT * FROM plantillas ORDER BY orden, id")
    protected abstract suspend fun plantillas(): List<Plantilla>

    @Query("SELECT * FROM trabajadores ORDER BY plantilla_id, orden, id")
    protected abstract suspend fun trabajadores(): List<Trabajador>

    @Query("SELECT * FROM tareas_plantilla ORDER BY plantilla_id, orden, id")
    protected abstract suspend fun tareasPlantilla(): List<TareaPlantilla>

    @Query("SELECT * FROM asignaciones ORDER BY fecha, trabajador_id")
    protected abstract suspend fun asignaciones(): List<Asignacion>

    @Query("SELECT * FROM ajustes ORDER BY clave")
    protected abstract suspend fun ajustes(): List<Ajuste>

    @Query("DELETE FROM asignaciones")
    protected abstract suspend fun borrarAsignaciones()

    @Query("DELETE FROM trabajadores")
    protected abstract suspend fun borrarTrabajadores()

    @Query("DELETE FROM tareas_plantilla")
    protected abstract suspend fun borrarTareasPlantilla()

    @Query("DELETE FROM tareas")
    protected abstract suspend fun borrarTareas()

    @Query("DELETE FROM listas")
    protected abstract suspend fun borrarListas()

    @Query("DELETE FROM plantillas")
    protected abstract suspend fun borrarPlantillas()

    @Query("DELETE FROM turnos")
    protected abstract suspend fun borrarTurnos()

    @Query("DELETE FROM ajustes")
    protected abstract suspend fun borrarAjustes()

    @Insert
    protected abstract suspend fun insertarTurnos(filas: List<Turno>)

    @Insert
    protected abstract suspend fun insertarListas(filas: List<Lista>)

    @Insert
    protected abstract suspend fun insertarTareas(filas: List<Tarea>)

    @Insert
    protected abstract suspend fun insertarPlantillas(filas: List<Plantilla>)

    @Insert
    protected abstract suspend fun insertarTrabajadores(filas: List<Trabajador>)

    @Insert
    protected abstract suspend fun insertarTareasPlantilla(filas: List<TareaPlantilla>)

    @Insert
    protected abstract suspend fun insertarAsignaciones(filas: List<Asignacion>)

    @Insert
    protected abstract suspend fun insertarAjustes(filas: List<Ajuste>)

    /** Consistent snapshot of every table. */
    @Transaction
    open suspend fun leerTodo(): DatosApp = DatosApp(
        turnos = turnos(),
        listas = listas(),
        tareas = tareas(),
        plantillas = plantillas(),
        trabajadores = trabajadores(),
        tareasPlantilla = tareasPlantilla(),
        asignaciones = asignaciones(),
        ajustes = ajustes(),
    )

    /**
     * Replaces ALL data with [datos], keeping their ids (so references stay valid). Children are
     * deleted before parents and inserted after them; any failure rolls everything back.
     */
    @Transaction
    open suspend fun reemplazarTodo(datos: DatosApp) {
        borrarAsignaciones()
        borrarTrabajadores()
        borrarTareasPlantilla()
        borrarTareas()
        borrarListas()
        borrarPlantillas()
        borrarTurnos()
        borrarAjustes()
        insertarPlantillas(datos.plantillas)
        insertarTrabajadores(datos.trabajadores)
        insertarTareasPlantilla(datos.tareasPlantilla)
        insertarListas(datos.listas)
        insertarTareas(datos.tareas)
        insertarAsignaciones(datos.asignaciones)
        insertarTurnos(datos.turnos)
        insertarAjustes(datos.ajustes)
    }
}
