package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.CLAVE_LISTA_CHECKLIST
import com.valanon.calendar.data.local.Transacciones
import com.valanon.calendar.data.local.dao.AjusteDao
import com.valanon.calendar.data.local.dao.ListaDao
import com.valanon.calendar.data.local.dao.PlantillaDao
import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.entity.Ajuste
import com.valanon.calendar.data.local.entity.Lista
import com.valanon.calendar.data.local.entity.ListaConProgreso
import com.valanon.calendar.data.model.TipoPlantilla
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Checklist lists and the active one, emitted together so they never disagree. */
data class ListasChecklist(
    val todas: List<ListaConProgreso>,
    val activa: ListaConProgreso?,
)

/**
 * The lists of the Checklist tab (spec 4.0, 4.5, 4.6) and which one is active (`ajustes`,
 * key [CLAVE_LISTA_CHECKLIST]). Lists created from a "Tareas" template get copies of its tasks.
 */
class ListasRepositorio(
    private val listaDao: ListaDao,
    private val tareaDao: TareaDao,
    private val plantillaDao: PlantillaDao,
    private val ajusteDao: AjusteDao,
    /** Multi-step writes below run atomically (all or nothing). */
    private val transacciones: Transacciones,
) {
    /**
     * Every list plus the active one: the saved id or, if none was saved or it was deleted, the
     * first list by `orden`; `null` when there are no lists.
     */
    val listas: Flow<ListasChecklist> =
        combine(listaDao.observarTodas(), ajusteDao.observar(CLAVE_LISTA_CHECKLIST)) { todas, guardada ->
            ListasChecklist(todas, todas.firstOrNull { it.lista.id == guardada?.toLongOrNull() } ?: todas.firstOrNull())
        }

    val listaActiva: Flow<Lista?> = listas.map { it.activa?.lista }

    suspend fun activar(listaId: Long) = ajusteDao.guardar(Ajuste(CLAVE_LISTA_CHECKLIST, listaId.toString()))

    /**
     * New list named after the "Tareas" template [plantillaId], with a copy of each of its tasks
     * (same text and order, unchecked), and makes it the active one. Returns its id, or `null` if
     * the template no longer exists or is not a task template.
     */
    suspend fun crearDesdePlantilla(plantillaId: Long): Long? = transacciones.ejecutar {
        val plantilla = plantillaDao.obtener(plantillaId)
        if (plantilla == null || plantilla.tipo != TipoPlantilla.TAREAS) return@ejecutar null
        val id = listaDao.insertarAlFinal(plantilla.nombre, plantilla.id)
        tareaDao.copiarDePlantilla(id, plantilla.id)
        activar(id)
        id
    }

    /**
     * New empty list named [nombre] (N) — "Lista N", N = number of lists + 1 — and makes it the
     * active one. Returns its id.
     */
    suspend fun crearEnBlanco(nombre: (Int) -> String): Long = transacciones.ejecutar {
        val id = listaDao.insertarAlFinal(nombre(listaDao.ids().size + 1), null)
        activar(id)
        id
    }

    /** Returns false (and keeps the old name) if [nombre] is blank. */
    suspend fun renombrar(id: Long, nombre: String): Boolean {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return false
        listaDao.renombrar(id, limpio)
        return true
    }

    /** Unchecks the tasks of one list; the other lists do not change. */
    suspend fun reiniciar(listaId: Long) = tareaDao.reiniciarLista(listaId)

    /**
     * Deletes the list and its tasks (cascade). If it was the active one, the next list becomes
     * active, or the previous one if it was the last (spec 4.0).
     */
    suspend fun borrar(id: Long) = transacciones.ejecutar {
        val ids = listaDao.ids()
        val posicion = ids.indexOf(id)
        if (posicion < 0) return@ejecutar
        val guardada = ajusteDao.obtener(CLAVE_LISTA_CHECKLIST)?.toLongOrNull()
        val activa = guardada?.takeIf { it in ids } ?: ids.first()
        listaDao.borrar(id)
        if (activa == id) {
            (ids.getOrNull(posicion + 1) ?: ids.getOrNull(posicion - 1))?.let { activar(it) }
        }
    }
}
