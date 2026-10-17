package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TrabajadorDao
import com.valanon.calendar.data.local.entity.Plantilla
import com.valanon.calendar.data.local.entity.PlantillaConRecuento
import com.valanon.calendar.data.local.entity.Trabajador
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.Flow

/**
 * Templates and their workers (spec 4b). Names are trimmed here; blank names never reach the
 * database (adding is ignored, renaming keeps the previous name).
 */
class PlantillasRepositorio(
    private val plantillaDao: PlantillaDao,
    private val trabajadorDao: TrabajadorDao,
) {
    val plantillas: Flow<List<PlantillaConRecuento>> = plantillaDao.observarTodas()

    fun plantilla(id: Long): Flow<Plantilla?> = plantillaDao.observar(id)

    fun trabajadores(plantillaId: Long): Flow<List<Trabajador>> = trabajadorDao.observarDePlantilla(plantillaId)

    /** Creates a template named "<tipo> N" (N = number of templates + 1) at the end. Returns its id. */
    suspend fun crear(tipo: TipoPlantilla): Long =
        plantillaDao.insertarAlFinal(tipo, "${tipo.etiqueta} ${plantillaDao.contar() + 1}")

    /** Returns false (and keeps the old name) if [nombre] is blank. */
    suspend fun renombrarPlantilla(id: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return false
        plantillaDao.renombrar(id, limpio)
        return true
    }

    /** Deletes the template and, by cascade, its workers. */
    suspend fun borrarPlantilla(id: Long) = plantillaDao.borrar(id)

    /** Adds [nombre] (trimmed) at the end of the template. Returns false if it is blank. */
    suspend fun agregarTrabajador(plantillaId: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return false
        trabajadorDao.agregarAlFinal(plantillaId, limpio)
        return true
    }

    /** Returns false (and keeps the old name) if [nombre] is blank. */
    suspend fun renombrarTrabajador(id: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return false
        trabajadorDao.renombrar(id, limpio)
        return true
    }

    suspend fun eliminarTrabajador(id: Long) = trabajadorDao.borrar(id)

    /** Undo of [eliminarTrabajador]: puts the worker back with its id and position. */
    suspend fun restaurarTrabajador(trabajador: Trabajador) {
        trabajadorDao.insertar(trabajador)
    }
}
