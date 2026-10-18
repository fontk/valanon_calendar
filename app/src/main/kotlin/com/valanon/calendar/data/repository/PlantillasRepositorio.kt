package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.Transacciones
import com.valanon.calendar.data.local.dao.AsignacionDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TareaPlantillaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.entity.Asignacion
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.TareaPlantilla
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.ElementoPlantilla
import com.valanon.calendar.data.model.TipoPlantilla
import com.valanon.calendar.data.model.comoElemento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Templates and their items (spec 4b): workers for [TipoPlantilla.TRABAJADORES], preset tasks for
 * [TipoPlantilla.TAREAS]. Item operations take the type and go to its table. Texts are trimmed
 * here; blank texts never reach the database (adding is ignored, renaming keeps the previous one).
 */
class PlantillasRepositorio(
    private val plantillaDao: PlantillaDao,
    private val trabajadorDao: TrabajadorDao,
    private val tareaPlantillaDao: TareaPlantillaDao,
    private val asignacionDao: AsignacionDao,
    /** Multi-step writes below run atomically (all or nothing). */
    private val transacciones: Transacciones,
) {
    val plantillas: Flow<List<PlantillaConRecuento>> = plantillaDao.observarTodas()

    fun plantilla(id: Long): Flow<Plantilla?> = plantillaDao.observar(id)

    fun elementos(tipo: TipoPlantilla, plantillaId: Long): Flow<List<ElementoPlantilla>> = when (tipo) {
        TipoPlantilla.TRABAJADORES -> trabajadorDao.observarDePlantilla(plantillaId).map { l -> l.map { it.comoElemento() } }
        TipoPlantilla.TAREAS -> tareaPlantillaDao.observarDePlantilla(plantillaId).map { l -> l.map { it.comoElemento() } }
    }

    /** Team shifts of the template's workers (Turnos tab), for the delete dialog. Always 0 for Tareas. */
    fun numeroAsignaciones(plantillaId: Long): Flow<Int> = asignacionDao.observarContarDePlantilla(plantillaId)

    /** Creates a template named "<tipo> N" (N = number of templates + 1) at the end. Returns its id. */
    suspend fun crear(tipo: TipoPlantilla): Long = transacciones.ejecutar {
        plantillaDao.insertarAlFinal(tipo, "${tipo.etiqueta} ${plantillaDao.contar() + 1}")
    }

    /** Returns false (and keeps the old name) if [nombre] is blank. */
    suspend fun renombrarPlantilla(id: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return false
        plantillaDao.renombrar(id, limpio)
        return true
    }

    /** Deletes the template and, by cascade, its items. Checklist lists copied from it stay. */
    suspend fun borrarPlantilla(id: Long) = plantillaDao.borrar(id)

    /** Adds [texto] (trimmed) at the end of the template. Returns false if it is blank. */
    suspend fun agregarElemento(tipo: TipoPlantilla, plantillaId: Long, texto: String): Boolean {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return false
        when (tipo) {
            TipoPlantilla.TRABAJADORES -> trabajadorDao.agregarAlFinal(plantillaId, limpio)
            TipoPlantilla.TAREAS -> tareaPlantillaDao.agregarAlFinal(plantillaId, limpio)
        }
        return true
    }

    /** Returns false (and keeps the old text) if [texto] is blank. */
    suspend fun cambiarTextoElemento(tipo: TipoPlantilla, id: Long, texto: String): Boolean {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return false
        when (tipo) {
            TipoPlantilla.TRABAJADORES -> trabajadorDao.renombrar(id, limpio)
            TipoPlantilla.TAREAS -> tareaPlantillaDao.cambiarTexto(id, limpio)
        }
        return true
    }

    /**
     * Deletes the item. A worker's team shifts go with it (ON DELETE CASCADE); they are returned,
     * read just before, so [restaurarElemento] can put them back. Preset tasks have none.
     */
    suspend fun eliminarElemento(tipo: TipoPlantilla, id: Long): List<Asignacion> = when (tipo) {
        TipoPlantilla.TRABAJADORES -> transacciones.ejecutar {
            val asignaciones = asignacionDao.obtenerDeTrabajador(id)
            trabajadorDao.borrar(id)
            asignaciones
        }
        TipoPlantilla.TAREAS -> {
            tareaPlantillaDao.borrar(id)
            emptyList()
        }
    }

    /** Undo of [eliminarElemento]: puts the item back with its id and position, and its shifts. */
    suspend fun restaurarElemento(
        tipo: TipoPlantilla,
        elemento: ElementoPlantilla,
        asignaciones: List<Asignacion> = emptyList(),
    ) {
        transacciones.ejecutar {
            when (tipo) {
                TipoPlantilla.TRABAJADORES -> {
                    trabajadorDao.insertar(Trabajador(elemento.id, elemento.plantillaId, elemento.texto, elemento.orden))
                    if (asignaciones.isNotEmpty()) asignacionDao.insertarTodas(asignaciones)
                }
                TipoPlantilla.TAREAS ->
                    tareaPlantillaDao.insertar(TareaPlantilla(elemento.id, elemento.plantillaId, elemento.texto, elemento.orden))
            }
        }
    }
}
