package com.valanon.calendar.data.repository

import com.valanon.calendar.data.local.dao.TareaDao
import com.valanon.calendar.data.local.entity.Tarea
import kotlinx.coroutines.flow.Flow

/** Tasks of one Checklist list. Text is trimmed here so blank tasks never reach the database. */
class TareasRepositorio(private val dao: TareaDao) {

    fun tareas(listaId: Long): Flow<List<Tarea>> = dao.observarDeLista(listaId)

    /** Adds [texto] (trimmed) at the end of the list. Returns false and does nothing if it is blank. */
    suspend fun agregar(listaId: Long, texto: String): Boolean {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return false
        dao.agregarAlFinal(listaId, limpio)
        return true
    }

    suspend fun marcar(id: Long, hecha: Boolean) = dao.actualizarHecha(id, hecha)

    suspend fun borrar(id: Long) = dao.borrar(id)

    /** Puts back a deleted task with its original id, state and position (unless its list is gone). */
    suspend fun restaurar(tarea: Tarea) = dao.restaurar(tarea.id, tarea.listaId, tarea.texto, tarea.hecha, tarea.orden)
}
